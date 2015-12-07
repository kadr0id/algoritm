/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util.http;

import com.google.common.base.Strings;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import scripting.util.concurrent.ManualResetEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Doan
 */
public class ProxyMonitor implements Runnable {

    public List<Proxy> proxies;
    public List<Proxy> aliveProxies;
    public ExecutorService service;
    private CountDownLatch l;
    private Semaphore sema;
    private ReentrantReadWriteLock aliveProxyLock = new ReentrantReadWriteLock();
    private Object allProxyLock = new Object();
    private ManualResetEvent checkProxyEvent = new ManualResetEvent(false);

    @Override
    public void run() {
        service = Executors.newFixedThreadPool(10);
        while (true) {
            try {
                checkProxyEvent.reset();
                checkProxy();
                //Thread.sleep(1000 * 60 * 5);
                checkProxyEvent.waitOne(5 * 60, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Logger.getLogger(ProxyMonitor.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
    }

    public void checkProxy() throws InterruptedException {
        final List<Proxy> allProxy;
        final List<Proxy> tempProxy;
        synchronized (allProxyLock) {
            allProxy = new ArrayList<Proxy>();
            for (int i = 0; i < this.proxies.size(); i++) {
                allProxy.add(this.proxies.get(i));
            }
            tempProxy = new ArrayList<Proxy>();
        }

        l = new CountDownLatch(allProxy.size());
        sema = new Semaphore(10);
        for (int i = 0; i < allProxy.size(); i++) {
            final Proxy p = allProxy.get(i);
            sema.acquire();
            service.submit(new Runnable() {
                @Override
                public void run() {
                    DefaultHttpClient client = new DefaultHttpClient();
                    try {
                        HttpParams params = client.getParams();
                        HttpConnectionParams.setConnectionTimeout(params, 30 * 1000);
                        HttpConnectionParams.setSoTimeout(params, 30 * 1000);
                        HttpHost proxy = new HttpHost(p.getAddress(), p.getPort());
                        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                        if (Strings.isNullOrEmpty(p.getUsername()) == false) {
                            client.getCredentialsProvider().setCredentials(new AuthScope(p.getAddress(), p.getPort()), new UsernamePasswordCredentials(p.getUsername(), p.getPassword()));
                        }
                        HttpGet request = new HttpGet("http://vnexpress.net/");
                        HttpResponse response = client.execute(request);
                        if (response.getStatusLine().getStatusCode() == 407
                                || response.getStatusLine().getStatusCode() == 403
                                || response.getStatusLine().getStatusCode() == 401) {
                            System.out.println("proxy fail:" + p.getAddress() + ":" + p.getPort() + ":" + p.getUsername() + ":" + p.getPassword());
                            return;
                        }
                        tempProxy.add(p);
                        System.out.println("proxy ok: " +  p.getAddress() + ":" + p.getPort() + ":" + p.getUsername() + ":" + p.getPassword());
                    } catch (Exception ex) {
                        System.out.println("exception at: " +  p.getAddress() + ":" + p.getPort() + ":" + p.getUsername() + ":" + p.getPassword());
                    } finally {
                        client.getConnectionManager().shutdown();
                        sema.release();
                        l.countDown();
                    }
                }
            });
        }
        l.await();
        aliveProxyLock.writeLock().lock();
        this.aliveProxies = tempProxy;
        aliveProxyLock.writeLock().unlock();
    }

    public Proxy getRandomProxy() {
        aliveProxyLock.readLock().lock();
        Random rnd = new Random();

        try {
            int rndIdx = rnd.nextInt(aliveProxies.size());
            Proxy p = aliveProxies.get(rndIdx);
            aliveProxyLock.readLock().unlock();
            return p;
        } catch (Exception ex) {
            aliveProxyLock.readLock().unlock();
            return null;
        }

    }

    public void setProxy(String proxyList) {
        synchronized (allProxyLock) {
            this.proxies = new ArrayList<Proxy>();
            String[] proxies = proxyList.split("\r\n");
            for (int i = 0; i < proxies.length; i++) {
                try {
                    String[] proxyDetails = proxies[i].split(":");
                    if (proxyDetails.length == 4) {
                        this.proxies.add(new Proxy(proxyDetails[0], Integer.valueOf(proxyDetails[1]), proxyDetails[2], proxyDetails[3]));
                    } else if (proxyDetails.length == 2) {
                        this.proxies.add(new Proxy(proxyDetails[0], Integer.valueOf(proxyDetails[1])));
                    }
                } catch (Exception ex) {
                }
            }
        }
        checkProxyEvent.set();
    }
}
