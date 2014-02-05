package org.boon.di.modules;

import org.boon.Exceptions;
import org.boon.Sets;
import org.boon.collections.MultiMap;
import org.boon.core.Supplier;
import org.boon.core.reflection.Reflection;
import org.boon.di.Module;
import org.boon.di.SupplierInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.boon.di.modules.NamedUtils.namedValueForClass;

/**
 * Created by Richard on 2/3/14.
 */
public class SupplierModule implements Module {

    private Map<Class, Supplier<Object>> supplierTypeMap = new ConcurrentHashMap<>();

    private MultiMap<String, SupplierInfo> supplierNameMap = new MultiMap<>();

    public SupplierModule( SupplierInfo... suppliers ) {
        supplierExtraction( suppliers );
    }


    public SupplierModule( Map<?, ?> map ) {
        List<SupplierInfo> list = new ArrayList<>(  );

        for (Map.Entry<?,?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            list.add( SupplierInfo.supplier( key, value  ));
        }

         supplierExtraction( list.toArray( new SupplierInfo[list.size()] ) );
    }




    @Override
    public <T> T get( Class<T> type ) {
        return ( T ) supplierTypeMap.get( type ).get();
    }

    @Override
    public Object get( String name ) {
        return supplierNameMap.get( name ).supplier().get();

    }

    @Override
    public <T> T get( Class<T> type, String name ) {

        return getSupplier( type, name ).get();
    }



    @Override
    public <T> Supplier<T> getSupplier( final Class<T> type, final String name ) {


        SupplierInfo nullInfo = null;

        try {
            Set<SupplierInfo> set = Sets.set( supplierNameMap.getAll( name ) );

            for ( SupplierInfo info : set ) {

                if ( info.type() == null ) {
                    nullInfo = info;
                    continue;
                }
                if ( type.isAssignableFrom( info.type() ) ) {
                    return (  Supplier<T> ) info.supplier();
                }
            }

            return (  Supplier<T> ) ( nullInfo != null ? nullInfo.supplier() :   new Supplier<T>() {
                @Override
                public T get() {
                    return null;
                }
            });

        } catch ( Exception e ) {
            Exceptions.handle( e );
            return null;
        }
    }



    @Override
    public <T> Supplier<T> getSupplier( Class<T> type ) {
        Supplier<T> supplier = ( Supplier<T> ) supplierTypeMap.get( type );
        if (supplier == null ) {
            supplier =  new Supplier<T>() {
                @Override
                public T get() {
                    return null;
                }
            };
        }

        return supplier;
    }

    @Override
    public boolean has( Class type ) {
        return supplierTypeMap.containsKey( type );
    }

    @Override
    public boolean has( String name ) {
        return supplierNameMap.containsKey( name );
    }


    private void extractClassIntoMaps( Class type, boolean foundName, Supplier supplier ) {

        if ( type == null ) {
            return;
        }
        String named = null;


        Class superClass = type.getSuperclass();


        Class[] superTypes = type.getInterfaces();

        for ( Class superType : superTypes ) {
            this.supplierTypeMap.put( superType, supplier );
        }

        while ( superClass != Object.class ) {
            this.supplierTypeMap.put( superClass, supplier );

            if ( !foundName ) {
                named = NamedUtils.namedValueForClass( superClass );
                if ( named != null ) {
                    supplierNameMap.put( named, new SupplierInfo( named, type, supplier, null ) );
                    foundName = true;
                }
            }

            superTypes = type.getInterfaces();
            for ( Class superType : superTypes ) {
                this.supplierTypeMap.put( superType, supplier );
            }
            superClass = superClass.getSuperclass();
        }


    }


    private void supplierExtraction( SupplierInfo[] suppliers ) {
        for ( SupplierInfo supplierInfo : suppliers ) {

            Class<?> type = supplierInfo.type();
            /* Get type from value. */
            if ( type == null && supplierInfo.value() != null ) {
                type = supplierInfo.value().getClass();

            }
            String named = supplierInfo.name();
            Supplier supplier = supplierInfo.supplier();

            if ( supplier == null ) {
                supplier = createSupplier( type, supplierInfo.value() );
            }

            if ( type != null ) {

                supplierTypeMap.put( type, supplier );


                /* Named passed in overrides name in class annotation @Named. */
                if ( named == null ) {

                    named = namedValueForClass( type );
                }
            }

            extractClassIntoMaps( type, named != null, supplier );
            if ( named != null ) {
                supplierNameMap.put( named, new SupplierInfo( named, type, supplier, null ) );
            }
        }
    }


    private Supplier createSupplier( final Class<?> type, final Object value ) {
        if ( value != null ) {
            return new Supplier() {
                @Override
                public Object get() {
                    return value;
                }
            };
        } else if ( type != null ) {
            return new Supplier() {
                @Override
                public Object get() {
                    return Reflection.newInstance( type );
                }
            };
        } else {
            return new Supplier() {
                @Override
                public Object get() {
                    return null;
                }
            };
        }
    }



}