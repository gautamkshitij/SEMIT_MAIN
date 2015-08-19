package org.zm;

import org.zm.CachierException.Reason;

public class Cachier {

    Linker linky = null;
    Copier copie = null;
    Sizer sizy = null;
    Mover movie = null;
    Deleter delly = null;

    /**
     * a list of the system component plugins. Should contain one of each and
     * listed in order of preference
     */
    SystemComponent tools[] = { (new FastCopy()), (new Junction()), };

    public Cachier() {
	try {
	    loadTools();
	} catch (CachierException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    private void loadTools() throws CachierException {
	for (SystemComponent s : tools) {
	    if (s.isAvailable()) {
		if (s instanceof Linker) {
		    if (linky == null)
			linky = (Linker) s;
		}
		if (s instanceof Copier) {
		    if (copie == null)
			copie = (Copier) s;
		}
		if (s instanceof Sizer) {
		    if (sizy == null)
			sizy = (Sizer) s;
		}
		if (s instanceof Mover) {
		    if (movie == null)
			movie = (Mover) s;
		}
		if (s instanceof Deleter) {
		    if (delly == null)
			delly = (Deleter) s;
		}
	    }
	}
	if (linky == null)
	    throw new CachierException(Reason.NO_LINKER);
	if (copie == null)
	    throw new CachierException(Reason.NO_COPIER);
	if (sizy == null)
	    throw new CachierException(Reason.NO_SIZER);
	if (movie == null)
	    throw new CachierException(Reason.NO_MOVER);
	if (delly == null)
	    throw new CachierException(Reason.NO_DELETER);

    }

    public static void load(CacheEntry cache, Linker link, Copier copy, Sizer size) throws CachierException {

	long sourceSize = size.usedSpace(cache.getSourceFolder());

	if (sourceSize > size.freeSpace(cache.getCacheFolder())) {
	    throw new CachierException(CachierException.Reason.NOT_ENOUGH_SPACE_IN_CACHE);
	} else {

	    copy.copy(cache.getDataStoreFolder(), cache.getCacheFolder());
	    link.link(cache.getSourceFolder(), cache.getCacheFolder());

	}

    }

    public static void unLoad(CacheEntry cache, Linker link, Copier copy, Deleter del) {

	copy.copy(cache.getCacheFolder(), cache.getDataStoreFolder());
	link.link(cache.getSourceFolder(), cache.getDataStoreFolder());
	del.delete(cache.getCacheFolder());

    }

    public static void sync(CacheEntry cache, Copier copy) {
	copy.copy(cache.getCacheFolder(), cache.getDataStoreFolder());

    }

    public static void main(String args[]) {

    }

}
