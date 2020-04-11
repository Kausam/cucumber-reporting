package net.masterthought.cucumber.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.json.support.Durationable;
import net.masterthought.cucumber.json.support.Status;
import net.masterthought.cucumber.json.support.StatusCounter;
import net.masterthought.cucumber.util.Util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Element implements Durationable {

    // Start: attributes from JSON file report
    private final String id = null;
    private final String name = null;
    private final String type = null;
    private final String description = null;
    private final String keyword = null;
    private final Integer line = null;

    /**
     * @since cucumber-jvm v4.3.0
     * The timestamp field in the json report allows plugins to correctly calculate
     * the time a TestSuite takes when the TCs are run in parallel.
     * The scenario startTime (which is what i currently added to the report) will be useful
     * in a number of ways both for reporting and getting correct duration of parallel TC execution.
     */
    @JsonProperty("start_timestamp")
    private final LocalDateTime startTime = null;
    private Step[] steps = new Step[0];
    private final Hook[] before = new Hook[0];
    private final Hook[] after = new Hook[0];
    private final Tag[] tags = new Tag[0];
    // End: attributes from JSON file report

    private static final String SCENARIO_TYPE = "scenario";
    private static final String BACKGROUND_TYPE = "background";

    private Status elementStatus;
    private Status beforeStatus;
    private Status afterStatus;
    private Status stepsStatus;

    private Feature feature;
    private long duration;

    public Step[] getSteps() {
        return steps;
    }

    public Hook[] getBefore() {
        return before;
    }

    public Hook[] getAfter() {
        return after;
    }

    public Tag[] getTags() {
        return tags;
    }

    public Status getStatus() {
        return elementStatus;
    }

    public Status getBeforeStatus() {
        return beforeStatus;
    }

    public Status getAfterStatus() {
        return afterStatus;
    }

    public Status getStepsStatus() {
        return stepsStatus;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKeyword() {
        return keyword;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Integer getLine() {
        return line;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return StringUtils.defaultString(description);
    }

    public boolean isScenario() {
        return SCENARIO_TYPE.equalsIgnoreCase(type);
    }

    public boolean isBackground() {
        return BACKGROUND_TYPE.equalsIgnoreCase(type);
    }

    public Feature getFeature() {
        return feature;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public String getFormattedDuration() {
        return Util.formatDuration(duration);
    }

    public void setMetaData(Feature feature, Configuration configuration) {
        this.feature = feature;

        for (Step step : steps) {
            step.setMetaData();
        }

        beforeStatus = new StatusCounter(before).getFinalStatus();
        afterStatus = new StatusCounter(after).getFinalStatus();
        stepsStatus = new StatusCounter(steps, configuration.getNotFailingStatuses()).getFinalStatus();
        elementStatus = calculateElementStatus();

        calculateDuration();
    }

    private Status calculateElementStatus() {
        StatusCounter statusCounter = new StatusCounter();
        statusCounter.incrementFor(stepsStatus);
        statusCounter.incrementFor(beforeStatus);
        statusCounter.incrementFor(afterStatus);
        return statusCounter.getFinalStatus();
    }

    private void calculateDuration() {
        for (Step step : steps) {
            duration += step.getResult().getDuration();
        }
    }

    /**
     * Depth of a step is given by the number of leading '>' symbols in the step's keyword
     * @param index
     * @return depth of the step present at index position of steps array
     */
    private int getStepDepth(int index) {
        if (org.apache.commons.lang3.StringUtils.isBlank(steps[index].getKeyword())) {
            return 0;
        }
        String keyword = steps[index].getKeyword();
        int depth = 0;
        while (depth < keyword.length() && keyword.charAt(depth) == '>') {
            depth++;
        }
        return depth;
    }

    /**
     * Groups the flat steps array into a hierarchical tree structure
     * Needs to be called at least once after Json deserialization
     */
    public void makeStepTree() {
        List<Step> resultStepList = new ArrayList<>();
        int index = 0;
        while (index < steps.length) {
            if (getStepDepth(index) == 0) {
                //The existing Step object at index location will be modified
                resultStepList.add(steps[index]);
                index = modifyStep(index);
            }
        }
        //this is the only place where we are destroying any step object
        steps = resultStepList.toArray(new Step[0]);
    }

    /**
     * Put the {@code steps[index]} step's children inside the {@code childSteps} array
     * @param index
     * @return
     */
    public int modifyStep(int index) {
        if (index >= steps.length-1)
            return steps.length;
        List<Step> childSteps = new ArrayList<>();
        int currentDepth = getStepDepth(index);
        int currentIndex = index;
        index++;
        while (index < steps.length) {
            int childDepth = getStepDepth(index);
            if (childDepth == currentDepth+1) {
                //The existing Step object will be modified by the modifyStep call
                childSteps.add(steps[index]);
                index = modifyStep(index);
            } else if (childDepth <= currentDepth) {
                break;
            }
        }
        steps[currentIndex].setChildSteps(childSteps.toArray(new Step[0]));
        return index;
    }
}
