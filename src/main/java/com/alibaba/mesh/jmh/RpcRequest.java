package com.alibaba.mesh.jmh;

import com.alibaba.mesh.demo.HttpInvoker;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author yiji.github@hotmail.com
 */

public class RpcRequest {

    @Benchmark
    @BenchmarkMode(Mode.All)
    @Measurement(iterations = 2, time = 60, timeUnit = TimeUnit.SECONDS)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Threads(10)
    public void measureHttpInvoke() throws InterruptedException {
        try{
            HttpInvoker.invoke();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RpcRequest.class.getSimpleName())
                .jvmArgs("-ea")
                .build();

        new Runner(opt).run();
    }

}
