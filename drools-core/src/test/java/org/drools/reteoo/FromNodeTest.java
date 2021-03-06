/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.reteoo;

import org.drools.Cheese;
import org.drools.FactHandle;
import org.drools.RuleBaseConfiguration;
import org.drools.RuleBaseFactory;
import org.drools.WorkingMemory;
import org.drools.base.ClassFieldAccessorCache;
import org.drools.base.ClassFieldAccessorStore;
import org.drools.base.ClassFieldReader;
import org.drools.base.ClassObjectType;
import org.drools.base.FieldFactory;
import org.drools.common.BetaConstraints;
import org.drools.common.DefaultFactHandle;
import org.drools.common.InternalFactHandle;
import org.drools.common.PropagationContextImpl;
import org.drools.common.SingleBetaConstraints;
import org.drools.reteoo.FromNode.FromMemory;
import org.drools.reteoo.builder.BuildContext;
import org.drools.rule.Declaration;
import org.drools.rule.From;
import org.drools.rule.MvelConstraintTestUtil;
import org.drools.rule.Pattern;
import org.drools.rule.constraint.MvelConstraint;
import org.drools.spi.AlphaNodeFieldConstraint;
import org.drools.spi.DataProvider;
import org.drools.spi.PropagationContext;
import org.drools.spi.Tuple;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class FromNodeTest {
    ClassFieldAccessorStore      store  = new ClassFieldAccessorStore();
    private ReteooRuleBase       ruleBase;
    private BuildContext         buildContext;

    @Before
    public void setUp() throws Exception {
        store.setClassFieldAccessorCache( new ClassFieldAccessorCache( Thread.currentThread().getContextClassLoader() ) );
        store.setEagerWire( true );

        ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase();
        buildContext = new BuildContext( ruleBase,
                                         new ReteooBuilder.IdGenerator() );
    }

    @Test
    public void testAlphaNode() {
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null,
                                                                       null );
        final ReteooWorkingMemory workingMemory = new ReteooWorkingMemory( 1,
                                                                           ruleBase );

        final ClassFieldReader extractor = store.getReader( Cheese.class,
                                                            "type",
                                                            getClass().getClassLoader() );

        final MvelConstraint constraint = new MvelConstraintTestUtil( "type == \"stilton\"",
                                                                      FieldFactory.getInstance().getFieldValue( "stilton" ),
                                                                      extractor );

        final List list = new ArrayList();
        final Cheese cheese1 = new Cheese( "cheddar",
                                           20 );
        final Cheese cheese2 = new Cheese( "brie",
                                           20 );
        list.add( cheese1 );
        list.add( cheese2 );
        final MockDataProvider dataProvider = new MockDataProvider( list );
        
        final Pattern pattern = new Pattern( 0,
                                             new ClassObjectType( Cheese.class ) );
        
        From fromCe = new From(dataProvider);
        fromCe.setResultPattern( pattern ); 

        final FromNode from = new FromNode( 3,
                                            dataProvider,
                                            new MockTupleSource( 80 ),
                                            new AlphaNodeFieldConstraint[]{constraint},
                                            null,
                                            true,
                                            buildContext,
                                            fromCe );
        final MockLeftTupleSink sink = new MockLeftTupleSink( 5 );
        from.addTupleSink( sink );

        final Person person1 = new Person( "xxx1",
                                           30 );
        final FactHandle person1Handle = workingMemory.insert( person1 );
        final LeftTuple tuple1 = new LeftTupleImpl( (DefaultFactHandle) person1Handle,
                                                    from,
                                                    true );
        from.assertLeftTuple( tuple1,
                              context,
                              workingMemory );

        // nothing should be asserted, as cheese1 is cheddar and we are filtering on stilton
        assertEquals( 0,
                      sink.getAsserted().size() );

        //Set cheese1 to stilton and it should now propagate
        cheese1.setType( "stilton" );
        final Person person2 = new Person( "xxx2",
                                           30 );
        final FactHandle person2Handle = workingMemory.insert( person2 );
        final LeftTuple tuple2 = new LeftTupleImpl( (DefaultFactHandle) person2Handle,
                                                    from,
                                                    true );
        from.assertLeftTuple( tuple2,
                              context,
                              workingMemory );

        final List asserted = sink.getAsserted();
        assertEquals( 1,
                      asserted.size() );
        Tuple tuple = (Tuple) ((Object[]) asserted.get( 0 ))[0];
        assertSame( person2,
                    tuple.getFactHandles()[1].getObject() );
        assertSame( cheese1,
                    tuple.getFactHandles()[0].getObject() );

        cheese2.setType( "stilton" );
        final Person person3 = new Person( "xxx2",
                                           30 );
        final FactHandle person3Handle = workingMemory.insert( person3 );
        final LeftTuple tuple3 = new LeftTupleImpl( (DefaultFactHandle) person3Handle,
                                                    from,
                                                    true );
        from.assertLeftTuple( tuple3,
                              context,
                              workingMemory );

        assertEquals( 3,
                      asserted.size() );
        tuple = (Tuple) ((Object[]) asserted.get( 1 ))[0];
        assertSame( person3,
                    tuple.getFactHandles()[1].getObject() );
        assertSame( cheese1,
                    tuple.getFactHandles()[0].getObject() );
        tuple = (Tuple) ((Object[]) asserted.get( 2 ))[0];
        assertSame( person3,
                    tuple.getFactHandles()[1].getObject() );
        assertSame( cheese2,
                    tuple.getFactHandles()[0].getObject() );

        assertNotSame( cheese1,
                       cheese2 );
    }

    @Test
    public void testBetaNode() {
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null,
                                                                       null );

        final ReteooWorkingMemory workingMemory = new ReteooWorkingMemory( 1,
                                                                           (ReteooRuleBase) RuleBaseFactory.newRuleBase() );

        final ClassFieldReader priceExtractor = store.getReader( Cheese.class,
                                                                 "price",
                                                                 getClass().getClassLoader() );

        final ClassFieldReader ageExtractor = store.getReader( Person.class,
                                                               "age",
                                                               getClass().getClassLoader() );

        final Pattern pattern = new Pattern( 0,
                                             new ClassObjectType( Person.class ) );

        final Declaration declaration = new Declaration( "age",
                                                         ageExtractor,
                                                         pattern );

        MvelConstraint variableConstraint = new MvelConstraintTestUtil("price == age", declaration, priceExtractor);

        final RuleBaseConfiguration configuration = new RuleBaseConfiguration();
        configuration.setIndexRightBetaMemory( false );
        configuration.setIndexLeftBetaMemory( false );
        final BetaConstraints betaConstraints = new SingleBetaConstraints( variableConstraint,
                                                                           configuration );

        final List list = new ArrayList();
        final Cheese cheese1 = new Cheese( "cheddar",
                                           18 );
        final Cheese cheese2 = new Cheese( "brie",
                                           12 );
        list.add( cheese1 );
        list.add( cheese2 );
        final MockDataProvider dataProvider = new MockDataProvider( list );
        
        From fromCe = new From(dataProvider);
        fromCe.setResultPattern( new Pattern( 0,
                                              new ClassObjectType( Cheese.class ) ) );

        final FromNode from = new FromNode( 3,
                                            dataProvider,
                                            new MockTupleSource( 40 ),
                                            new AlphaNodeFieldConstraint[0],
                                            betaConstraints,
                                            true,
                                            buildContext,
                                            fromCe );
        final MockLeftTupleSink sink = new MockLeftTupleSink( 5 );
        from.addTupleSink( sink );

        final Person person1 = new Person( "xxx1",
                                           30 );
        final FactHandle person1Handle = workingMemory.insert( person1 );
        final LeftTuple tuple1 = new LeftTupleImpl( (DefaultFactHandle) person1Handle,
                                                    from,
                                                    true );
        from.assertLeftTuple( tuple1,
                              context,
                              workingMemory );

        // nothing should be asserted, as cheese1 is cheddar and we are filtering on stilton
        assertEquals( 0,
                      sink.getAsserted().size() );

        //Set cheese1 to stilton and it should now propagate
        cheese1.setPrice( 30 );
        final Person person2 = new Person( "xxx2",
                                           30 );
        final FactHandle person2Handle = workingMemory.insert( person2 );
        final LeftTuple tuple2 = new LeftTupleImpl( (DefaultFactHandle) person2Handle,
                                                    from,
                                                    true );
        from.assertLeftTuple( tuple2,
                              context,
                              workingMemory );

        final List asserted = sink.getAsserted();
        assertEquals( 1,
                      asserted.size() );
        Tuple tuple = (Tuple) ((Object[]) asserted.get( 0 ))[0];
        assertSame( person2,
                    tuple.getFactHandles()[1].getObject() );
        assertSame( cheese1,
                    tuple.getFactHandles()[0].getObject() );

        cheese2.setPrice( 30 );
        final Person person3 = new Person( "xxx2",
                                           30 );
        final FactHandle person3Handle = workingMemory.insert( person3 );
        final LeftTuple tuple3 = new LeftTupleImpl( (DefaultFactHandle) person3Handle,
                                                    from,
                                                    true );
        from.assertLeftTuple( tuple3,
                              context,
                              workingMemory );

        assertEquals( 3,
                      asserted.size() );
        tuple = (Tuple) ((Object[]) asserted.get( 1 ))[0];
        assertSame( person3,
                    tuple.getFactHandles()[1].getObject() );
        assertSame( cheese1,
                    tuple.getFactHandles()[0].getObject() );
        tuple = (Tuple) ((Object[]) asserted.get( 2 ))[0];
        assertSame( person3,
                    tuple.getFactHandles()[1].getObject() );
        assertSame( cheese2,
                    tuple.getFactHandles()[0].getObject() );

        assertNotSame( cheese1,
                       cheese2 );
    }

    @Test
    public void testRestract() {
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null,
                                                                       null );
        final ReteooWorkingMemory workingMemory = new ReteooWorkingMemory( 1,
                                                                           (ReteooRuleBase) RuleBaseFactory.newRuleBase() );
        final ClassFieldReader extractor = store.getReader( Cheese.class,
                                                            "type",
                                                            getClass().getClassLoader() );

        final MvelConstraint constraint = new MvelConstraintTestUtil( "type == \"stilton\"",
                                                                      FieldFactory.getInstance().getFieldValue("stilton"),
                                                                      extractor );

        final List list = new ArrayList();
        final Cheese cheese1 = new Cheese( "stilton",
                                           5 );
        final Cheese cheese2 = new Cheese( "stilton",
                                           15 );
        list.add( cheese1 );
        list.add( cheese2 );
        final MockDataProvider dataProvider = new MockDataProvider( list );
        
        final Pattern pattern = new Pattern( 0,
                                             new ClassObjectType( Cheese.class ) );
        
        From fromCe = new From(dataProvider);
        fromCe.setResultPattern( pattern );        

        final FromNode from = new FromNode( 3,
                                            dataProvider,
                                            new MockTupleSource( 30 ),
                                            new AlphaNodeFieldConstraint[]{constraint},
                                            null,
                                            true,
                                            buildContext,
                                            fromCe );
        final MockLeftTupleSink sink = new MockLeftTupleSink( 5 );
        from.addTupleSink( sink );

        final List asserted = sink.getAsserted();

        final Person person1 = new Person( "xxx2",
                                           30 );
        final FactHandle person1Handle = workingMemory.insert( person1 );
        final LeftTuple tuple = new LeftTupleImpl( (DefaultFactHandle) person1Handle,
                                                   from,
                                                   true );
        from.assertLeftTuple( tuple,
                              context,
                              workingMemory );

        assertEquals( 2,
                      asserted.size() );

        final FromMemory memory = (FromMemory) workingMemory.getNodeMemory( from );
        assertEquals( 1,
                      memory.betaMemory.getLeftTupleMemory().size() );
        assertNull( memory.betaMemory.getRightTupleMemory() );
        RightTuple rightTuple2 = tuple.getFirstChild().getRightParent();
        RightTuple rightTuple1 = tuple.getFirstChild().getLeftParentNext().getRightParent();
        assertFalse( rightTuple1.equals( rightTuple2 ) );
        assertNull( tuple.getFirstChild().getLeftParentNext().getLeftParentNext() );

        final InternalFactHandle handle2 = rightTuple2.getFactHandle();
        final InternalFactHandle handle1 = rightTuple1.getFactHandle();
        assertEquals( handle1.getObject(),
                      cheese2 );
        assertEquals( handle2.getObject(),
                      cheese1 );

        from.retractLeftTuple( tuple,
                               context,
                               workingMemory );
        assertEquals( 0,
                      memory.betaMemory.getLeftTupleMemory().size() );
        assertNull( memory.betaMemory.getRightTupleMemory() );
    }
    
    @Test
    public void testAssignable() {
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null,
                                                                       null );
        final ReteooWorkingMemory workingMemory = new ReteooWorkingMemory( 1,
                                                                           ruleBase );


        final List list = new ArrayList();
//        final Cheese cheese1 = new Cheese( "cheddar",
//                                           20 );
//        final Cheese cheese2 = new Cheese( "brie",
//                                           20 );
//        list.add( cheese1 );
//        list.add( cheese2 );
        
        Human h1  = new Human();
        Human h2  = new Human();
        Person p1 = new Person("darth", 105);
        Person p2 = new Person("yoda", 200);
        Person m1 = new Man("bobba", 95);
        Person m2 = new Man("luke", 40); 
        
        list.add(h1);
        list.add(h2);
        list.add(p1);
        list.add(p1);
        list.add(m1);
        list.add(m2);
        
        
        
        final MockDataProvider dataProvider = new MockDataProvider( list );
        
        final Pattern pattern = new Pattern( 0,
                                             new ClassObjectType( Person.class ) );
        
        From fromCe = new From(dataProvider);
        fromCe.setResultPattern( pattern ); 

        final FromNode from = new FromNode( 3,
                                            dataProvider,
                                            new MockTupleSource( 90 ),
                                            new AlphaNodeFieldConstraint[]{},
                                            null,
                                            true,
                                            buildContext,
                                            fromCe );
        
        final MockLeftTupleSink sink = new MockLeftTupleSink( 5 );
        from.addTupleSink( sink );

        final FactHandle handle = workingMemory.insert( "xxx" );
        final LeftTuple tuple1 = new LeftTupleImpl( (DefaultFactHandle) handle,
                                                    from,
                                                    true );
        from.assertLeftTuple( tuple1,
                              context,
                              workingMemory );
        
        List asserted = sink.getAsserted();
        int countHuman = 0;
        int countPerson = 0;
        int countMan = 0;
        for ( int i = 0; i < 4; i++ ) {
            Object o = ((LeftTuple) ((Object[]) asserted.get( i ))[0]).getLastHandle().getObject();
            if ( o.getClass() ==  Human.class ) {
                countHuman++;
            } else if ( o.getClass() == Person.class ) {
                countPerson++;
            } else  if ( o.getClass() == Man.class ) {
                countMan++;
            } 
        }
        
        assertEquals( 0, countHuman );
        assertEquals( 2, countPerson );
        assertEquals( 2, countMan );
    }    

    public static class MockDataProvider
        implements
        DataProvider {

        private static final long serialVersionUID = 510l;

        private Collection        collection;

        public Declaration[] getRequiredDeclarations() {
            return null;
        }

        public MockDataProvider(final Collection collection) {
            this.collection = collection;
        }

        public Iterator getResults(final Tuple tuple,
                                   final WorkingMemory wm,
                                   final PropagationContext ctx,
                                   final Object providerContext) {
            return this.collection.iterator();
        }

        public Object createContext() {
            return null;
        }

        public DataProvider clone() {
            return this;
        }

        public void replaceDeclaration(Declaration declaration,
                                       Declaration resolved) {
        }
    }

    
    public static class Human {
        
    }
    public static class Person extends Human {
        private String name;
        private int    age;

        public Person(final String name,
                      final int age) {
            super();
            this.name = name;
            this.age = age;
        }

        public int getAge() {
            return this.age;
        }

        public String getName() {
            return this.name;
        }
    }
    
    public static class Man extends Person {

        public Man(String name,
                   int age) {
            super( name,
                   age );
        }
        
    }
}
