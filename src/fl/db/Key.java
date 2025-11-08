package fl.db;

public class Key implements Comparable<Key> {
	public Key(long... keys) {
		this.keys = new long[keys.length];
		for (int i = 0; i < keys.length; ++i) this.keys[i] = keys[i];
	}
	
	@Override
	public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Key)){
            return false;
        }
        
        Key other = (Key)obj;
        if (this.keys.length != other.keys.length) return false;
        for (int i = 0; i < keys.length; ++i) {
        	if (this.keys[i] != other.keys[i]) return false;
        }
        return true;
	}
	
    @Override
    public int hashCode() {
        final int prime = 31;
        long result = 1;
        for (int i = 0; i < keys.length; ++i) {
          result = prime * result + keys[i];
        }
        return (int) result;
    }
    
	@Override
	public int compareTo(Key that) {
        if (this.keys.length != that.keys.length) {
        	System.err.println("Comparing two different type of keys " + this + " vs. " + that);
        	System.exit(0);
        }
        for (int i = 0; i < keys.length; ++i) {
        	if (this.keys[i] < that.keys[i]) return -1;
        	if (this.keys[i] > that.keys[i]) return 1;
        }
		return 0;
	}
	
	@Override
	public String toString() {
		String str = "Key [";
        for (int i = 0; i < keys.length; ++i) {
        	str = str + (i == 0 ? "" : ", ") + "key" + i + "=" + keys[i];
        }
		return str + "]";
	}
	
    public long[] keys = null;
}
