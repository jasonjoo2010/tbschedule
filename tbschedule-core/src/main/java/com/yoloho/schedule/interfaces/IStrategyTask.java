package com.yoloho.schedule.interfaces;

public interface IStrategyTask {
    public void initialTaskParameter(String strategyName, String taskParameter) throws Exception;

    public void stop(String strategyName) throws Exception;
}
