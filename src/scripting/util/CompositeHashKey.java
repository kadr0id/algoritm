/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util;

public class CompositeHashKey<K1, K2> {

    public K1 key1;
    public K2 key2;

    public CompositeHashKey(K1 key1, K2 key2) {
        this.key1 = key1;
        this.key2 = key2;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + key1.hashCode();
        hash = hash * 31 + key2.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        Enum t;
        if (obj == null) {
            return false;
        }
        if (obj instanceof CompositeHashKey) {
            CompositeHashKey tmpObj = (CompositeHashKey) obj;
            if (this.key1.equals(tmpObj.key1) && this.key2.equals(tmpObj.key2)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}