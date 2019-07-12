package com.yoloho.schedule.types;

import java.util.concurrent.atomic.AtomicLong;

public class StatisticsInfo {
	private AtomicLong fetchDataNum = new AtomicLong(0);//读取次数
	private AtomicLong fetchDataCount = new AtomicLong(0);//读取的数据量
	private AtomicLong dealDataSucess = new AtomicLong(0);//处理成功的数据量
	private AtomicLong dealDataFail = new AtomicLong(0);//处理失败的数据量
	private AtomicLong dealSpendTime = new AtomicLong(0);//处理总耗时,没有做同步，可能存在一定的误差
	private AtomicLong otherCompareCount = new AtomicLong(0);//特殊比较的次数

    public void addFetchDataNum(long value) {
        this.fetchDataNum.addAndGet(value);
    }

    public void addFetchDataCount(long value) {
        this.fetchDataCount.addAndGet(value);
    }

    public void addDealDataSucess(long value) {
        this.dealDataSucess.addAndGet(value);
    }

    public void addDealDataFail(long value) {
        this.dealDataFail.addAndGet(value);
    }

    public void addDealSpendTime(long value) {
        this.dealSpendTime.addAndGet(value);
    }

    public void addOtherCompareCount(long value) {
        this.otherCompareCount.addAndGet(value);
    }
    public String getDealDescription(){
        return new StringBuilder("FetchDataCount=").append(this.fetchDataCount)
                .append(",FetchDataNum=").append(this.fetchDataNum)
                .append(",DealDataSucess=").append(this.dealDataSucess)
                .append(",DealDataFail=").append(this.dealDataFail)
                .append(",DealSpendTime=").append(this.dealSpendTime)
                .append(",otherCompareCount=").append(this.otherCompareCount)
                .toString();  
    }

}