package com.km.BottleCapCollector.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SimilarityModel {

    private static final Logger logger = LogManager.getLogger(SimilarityModel.class);

    private int from00To10;
    private int from10To20;
    private int from20To30;
    private int from30To40;
    private int from40To50;
    private int from50To60;
    private int from60To70;
    private int from70To80;
    private int from80To90;
    private int from90To100;
    private boolean duplicate;

    private final int similarCapAmount = 4;


    private Set<HistogramResult> similarCaps = new CapTreeSet(
            Comparator.comparingDouble(o -> ((HistogramResult) o).getSimilarity()).reversed()
    );

    public class CapTreeSet extends TreeSet<HistogramResult> {

        private static final long serialVersionUID = 1L;

        public CapTreeSet(Comparator<Object> comparator) {
            super(comparator);
        }


        @Override
        public synchronized boolean add(HistogramResult e) {
            switch ((int) (e.getSimilarity() * 10)) {
                case 0:
                    from00To10++;
                    break;
                case 1:
                    from10To20++;
                    break;
                case 2:
                    from20To30++;
                    break;
                case 3:
                    from30To40++;
                    break;
                case 4:
                    from40To50++;
                    break;
                case 5:
                    from50To60++;
                    break;
                case 6:
                    from60To70++;
                    break;
                case 7:
                    from70To80++;
                    break;
                case 8:
                    from80To90++;
                    break;
                case 9:
                    from90To100++;
                    break;
                default:
            }
            return super.add(e);
        }
    }

    public int getFrom00To10() {
        return from00To10;
    }

    public void setFrom00To10(int from00To10) {
        this.from00To10 = from00To10;
    }

    public int getFrom10To20() {
        return from10To20;
    }

    public void setFrom10To20(int from10To20) {
        this.from10To20 = from10To20;
    }

    public int getFrom20To30() {
        return from20To30;
    }

    public void setFrom20To30(int from20To30) {
        this.from20To30 = from20To30;
    }

    public int getFrom30To40() {
        return from30To40;
    }

    public void setFrom30To40(int from30To40) {
        this.from30To40 = from30To40;
    }

    public int getFrom40To50() {
        return from40To50;
    }

    public void setFrom40To50(int from40To50) {
        this.from40To50 = from40To50;
    }

    public int getFrom50To60() {
        return from50To60;
    }

    public void setFrom50To60(int from50To60) {
        this.from50To60 = from50To60;
    }

    public int getFrom60To70() {
        return from60To70;
    }

    public void setFrom60To70(int from60To70) {
        this.from60To70 = from60To70;
    }

    public int getFrom70To80() {
        return from70To80;
    }

    public void setFrom70To80(int from70To80) {
        this.from70To80 = from70To80;
    }

    public int getFrom80To90() {
        return from80To90;
    }

    public void setFrom80To90(int from80To90) {
        this.from80To90 = from80To90;
    }

    public int getFrom90To100() {
        return from90To100;
    }

    public void setFrom90To100(int from90To100) {
        this.from90To100 = from90To100;
    }

    public Set<HistogramResult> getSimilarCaps() {
        return similarCaps;
    }

    public void setSimilarCaps(Set<HistogramResult> similarCaps) {
        this.similarCaps = similarCaps;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public void addValue(HistogramResult result) {
        similarCaps.add(result);
    }

    public Set<HistogramResult> calculateTopSimilar() {
        logger.info("Entering calculateTopSimilar() method");
        synchronized (similarCaps) {
            if (isDuplicate()) {
                markModelAsDuplicate(similarCaps.size());
                return new TreeSet<>();
            }
            return similarCaps.stream().limit(similarCapAmount).collect(Collectors.toSet());
        }
    }

    public void markModelAsDuplicate(int capCount) {
        this.setFrom00To10(0);
        this.setFrom10To20(0);
        this.setFrom20To30(0);
        this.setFrom30To40(0);
        this.setFrom40To50(0);
        this.setFrom50To60(0);
        this.setFrom60To70(0);
        this.setFrom70To80(0);
        this.setFrom80To90(0);
        this.setFrom90To100(capCount);
        logger.info("Cap marked as duplicate");
    }
}
