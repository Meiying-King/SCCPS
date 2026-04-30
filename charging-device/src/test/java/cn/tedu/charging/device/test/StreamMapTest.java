package cn.tedu.charging.device.test;

import java.util.List;
import java.util.stream.Collectors;

public class StreamMapTest {
    public static void main(String[] args) {
        List<Integer> nums=List.of(1,2,3,4,5);
        //使用流做映射
        List<Integer> newNums = nums.stream().map(num -> {
            //映射计算
            return num * num;
        }).collect(Collectors.toList());


    }
}
