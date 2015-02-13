import java.util.List;

public final class A
    extends X
    implements Y, java.io.Serializable
{
}

abstract class X
{
    public static final String X = "X";
    
    private int x,
                y[];
    
    public X() {}
    
    protected X(int x) {}

    public final int f() { return 0; }
    
    protected void f(Y[] y, java.lang.String s[]) {}

    String f(boolean b)[] { return null; }
    
    private static <T> java.util.Map<String, T> g(List<? extends T> arg)
        throws Exception { return null; }

    private @interface A
    {
        String A = "A";
        
        int[] value();
        
        public static enum E
            implements Y
        {
            E1, E2;
            
            static final String E = "E";
    
            E() {}
    
            public int f() { return 0; }
            
            private interface I {}
        }
    }
}

interface Y
    extends java.io.Serializable
{
    String Y = "Y";
    
    abstract int f();
    
    final class Z {}
}
