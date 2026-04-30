package cn.tedu.charging.device.test;

public class LoopNumTest {
    public static void main(String[] args) {
        Integer total=5473005;
        Integer batch=1000;

        Integer number=total%batch;
        System.out.println( number);
        Integer loopNum=number==0?total/batch:total/batch+1;
        System.out.println(loopNum);
    }
}
