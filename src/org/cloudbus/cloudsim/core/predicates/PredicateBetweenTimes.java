
/// Saeedeh added this to using for cancelling outdated event


package org.cloudbus.cloudsim.core.predicates;

import org.cloudbus.cloudsim.core.SimEvent;

public class PredicateBetweenTimes extends Predicate {
    private final int tag;
    private final double startTime;
    private final double endTime;

    public PredicateBetweenTimes(int tag, double startTime, double endTime) {
        this.tag = tag;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public boolean match(SimEvent ev) {
        return ev.getTag() == tag && ev.eventTime() > startTime && ev.eventTime() <= endTime;
    }
}
