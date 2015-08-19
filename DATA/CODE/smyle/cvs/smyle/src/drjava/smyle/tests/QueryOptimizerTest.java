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

package drjava.smyle.tests;

import junit.framework.*;
import drjava.smyle.core.indexing.*;
import drjava.smyle.meta.*;
import drjava.smyle.testtypes.*;

public class QueryOptimizerTest extends TestCase {
  public QueryOptimizerTest(String name) { super(name); }
  
  static final Function[] nil = new Function[0];
  static final IndexProfile
    profile_nil_nil = new IndexProfile(nil, nil),
    profile_l_nil = new IndexProfile(new Function[] { DataTypes.f_l }, nil),
    profile_nil_l = new IndexProfile(nil, new Function[] { DataTypes.f_l }),
    profile_l_ll  = new IndexProfile(new Function[] { DataTypes.f_l }, new Function[] { DataTypes.f_ll }),
    profile_b$s_l$ll = new IndexProfile(
        new Function[] { DataTypes.f_b, DataTypes.f_s }, 
        new Function[] { DataTypes.f_l, DataTypes.f_ll }),
    profile_l$ll_nil  = new IndexProfile(new Function[] { DataTypes.f_l, DataTypes.f_ll }, nil);
        
  static final IndexProfile[]
    someProfiles = { profile_nil_nil, profile_l_nil, profile_nil_l, profile_b$s_l$ll };
    
  static final Function[][]
    someFieldLists = { nil, { DataTypes.f_l }, { DataTypes.f_l, DataTypes.f_s } };
  
  public void testCreateProfile() {
    assertEquals(new IndexProfile(nil, nil),
      QueryOptimizer.createProfile(new Filter()));
    assertEquals(profile_l_nil,
      QueryOptimizer.createProfile(new DataTypes_filter().lEquals(0)));
    assertEquals(profile_nil_l,
      QueryOptimizer.createProfile(new DataTypes_filter().orderByL()));
    assertEquals(profile_b$s_l$ll,
      QueryOptimizer.createProfile(new DataTypes_filter()
        .bEquals(true).sEquals("test")
        .orderByL().orderByLl()));
  }
  
  public void testIndexProfileEquality() {
    assertEquals(profile_nil_nil, profile_nil_nil);
    assertTrue(!profile_nil_nil
        .equals(new IndexProfile(new Function[] {new Field(0)}, nil)));
    assertTrue(!profile_nil_nil
        .equals(new IndexProfile(nil, new Function[] {new Field(0)})));
  }
  
  public void testIndexProfileHashCode() {
    assertEquals(profile_nil_nil.hashCode(), new IndexProfile(nil, nil).hashCode());
    assertEquals(profile_nil_l.hashCode(), new IndexProfile(nil, new Function[] { DataTypes.f_l }).hashCode());
  }
  
  public void testIndexProfileToString() {
    assertEquals("perm=[] ord=[]", profile_nil_nil.toString());
    assertEquals("perm=[Field #4 Field #1] ord=[Field #3 Field #2]",
      new IndexProfile(new Function[] { DataTypes.f_strct, DataTypes.f_l },
      new Function[] { DataTypes.f_s, DataTypes.f_ll }).toString());
  }
  
  public void testIndexScore() {
    // nil profile always gives score 0
    for (int i = 0; i < someFieldLists.length; i++)
      assertEquals(0, QueryOptimizer.indexScore(profile_nil_nil, someFieldLists[i]));
      
    // empty field list always gives score 0
    for (int i = 0; i < someProfiles.length; i++)
      assertEquals(0, QueryOptimizer.indexScore(someProfiles[i], nil));
    
    assertEquals(1, QueryOptimizer.indexScore(profile_l_nil, new Function[] { DataTypes.f_l }));
    assertEquals(1, QueryOptimizer.indexScore(profile_l_nil,
      new Function[] { DataTypes.f_l, DataTypes.f_ll }));
      
    // index can't be used because first field isn't in profile
    assertEquals(0, QueryOptimizer.indexScore(profile_l_nil,
      new Function[] { DataTypes.f_ll, DataTypes.f_l }));
    
    assertEquals(2, QueryOptimizer.indexScore(profile_l_ll,
      new Function[] { DataTypes.f_l, DataTypes.f_ll }));
      
    assertEquals(2, QueryOptimizer.indexScore(profile_l$ll_nil,
      new Function[] { DataTypes.f_l, DataTypes.f_ll, DataTypes.f_strct }));
    assertEquals(2, QueryOptimizer.indexScore(profile_l$ll_nil,
      new Function[] { DataTypes.f_ll, DataTypes.f_l, DataTypes.f_strct }));
      
    // wrong order in ord part => only 2 points
    assertEquals(2, QueryOptimizer.indexScore(profile_b$s_l$ll,
      new Function[] { DataTypes.f_b, DataTypes.f_s, DataTypes.f_ll }));
  }
}
