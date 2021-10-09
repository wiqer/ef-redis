import org.junit.Test;

public class InstanceofTest1{
    @Test
    public void test(){
        InstanceofTest2 test=new InstanceofTest2();
        Inst instance=(Inst)test;
        System.out.println(test instanceof Inst);
        System.out.println(test instanceof Test1);
        System.out.println(instance instanceof Test1);

    }
    interface Inst{

    }
    interface Test1{

    }
    class InstanceofTest2  implements Inst,Test1{

    }
}
