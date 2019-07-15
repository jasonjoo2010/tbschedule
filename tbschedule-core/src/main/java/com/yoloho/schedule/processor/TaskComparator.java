package com.yoloho.schedule.processor;

import java.util.Comparator;

class TaskComparator<T> implements Comparator<T> {
    Comparator<T> comparator;

    public TaskComparator(Comparator<T> aComparator) {
        this.comparator = aComparator;
    }

    public int compare(T o1, T o2) {
        return this.comparator.compare(o1, o2);
    }

    public boolean equals(Object obj) {
        return this.comparator.equals(obj);
    }
}