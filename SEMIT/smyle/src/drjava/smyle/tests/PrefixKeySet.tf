/*
This source file is part of Smyle, a database library.
For up-to-date information, see http://www.drjava.de/smyle
Copyright (C) 2001 Stefan Reich (doc@drjava.de)

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For full license text, see doc/license/lgpl.txt in this distribution
*/

import drjava.smyle.core.*;
import org.artsProject.mcop.Buffer;

emptyKey = new UniversalKey(new Buffer("00000000"));
key1 = new UniversalKey(33);
key2 = new UniversalKey(55);

empty = new PrefixKeySet(emptyKey);
assertTrue(empty.contains(emptyKey));
assertTrue(empty.contains(key1));
assertTrue(empty.overlapsWithRange(key1, key1));
assertTrue(empty.overlapsWithRange(key1, key2));
assertTrue(empty.overlapsWithRange(key2, key2));
assertTrue(empty.overlapsWithRange(null, null));

keyset = new PrefixKeySet(key1);
assertFalse(keyset.contains(emptyKey));
assertTrue (keyset.contains(key1));
assertFalse(keyset.contains(key2));
assertTrue (keyset.overlapsWithRange(key1, key1));
assertTrue (keyset.overlapsWithRange(key1, key2));
assertFalse(keyset.overlapsWithRange(key2, key2));
assertTrue (keyset.overlapsWithRange(null, null));

