package org.apache.http.entity.mime.content;

import org.apache.http.entity.mime.MIME;
import scripting.util.TextUtility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: macintoshhd
 * Date: 10/16/13
 * Time: 1:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class VirtualFileBody extends AbstractContentBody {

    private final String filename;
    private final String charset;
    private byte[] data = null;

    /**
     * @since 4.1
     */
    public VirtualFileBody(final byte[] data,
                           final String filename,
                           final String mimeType,
                           final String charset) {
        super(mimeType);
        if (data == null) {
            throw new IllegalArgumentException("Virtual File Data may not be null");
        }
        this.data = data;
        if (filename != null)
            this.filename = filename;
        else
            this.filename = TextUtility.GetRandomChars(10) + ".jpg";
        this.charset = charset;
    }

    /**
     * @since 4.1
     */
    public VirtualFileBody(final byte[] data,
                           final String mimeType,
                           final String charset) {
        this(data, null, mimeType, charset);
    }

    public VirtualFileBody(final byte[] data, final String mimeType) {
        this(data, mimeType, null);
    }

    public VirtualFileBody(final byte[] data) {
        this(data, "application/octet-stream");
    }

    public void writeTo(final OutputStream out) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        InputStream in = new ByteArrayInputStream(this.data);
        try {
            byte[] tmp = new byte[4096];
            int l;
            while ((l = in.read(tmp)) != -1) {
                out.write(tmp, 0, l);
            }
            out.flush();
        } finally {
            in.close();
        }
    }

    public String getTransferEncoding() {
        return MIME.ENC_BINARY;
    }

    public String getCharset() {
        return charset;
    }

    public long getContentLength() {
        return this.data.length;
    }

    public String getFilename() {
        return filename;
    }

}
