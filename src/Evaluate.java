import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Created by ghostof2007 on 5/8/14.
 * Different evaluation routines
 */
public class Evaluate {

    static HashMap<String, String> predictedSegmentations = new HashMap<String, String>();
    static HashMap<String, String> incorrectSegmentations = new HashMap<String, String>();
    static HashMap<String, String> correctSegmentations = new HashMap<String, String>();

    static HashSet<Integer> getSegPoints(String segmentation) {
        HashSet<Integer> segPoints = new HashSet<Integer>();
        int i=0;
        for(char ch : segmentation.toCharArray()) {
            if(ch == '-')
                segPoints.add(i);
            else
                i++;
        }
        return segPoints;
    }

    static double [] evaluateSegmentationPoints(String predSeg, ArrayList<String> goldSegs) {
        //find the best match over different points
        System.out.println();
        System.out.println("Evaluating " + predSeg);
        System.out.println("goldSegs: " + goldSegs);

        double bestCorrect = 0., bestTotal = 0., minBestTotal = 100.;
        HashSet<Integer> predPoints = getSegPoints(predSeg);
        int predSize = predPoints.size();

        for(String goldSeg : goldSegs) {
            HashSet<Integer> goldPoints = getSegPoints(goldSeg);

            System.out.println("Points:");
            System.out.println("Pred: " + predPoints);
            System.out.println("Gold: " + goldPoints);

            // Hur många segment har guldsegmenteringen?
            int goldSize = goldPoints.size();
            goldPoints.retainAll(predPoints); //IMP  : goldPoints is modified here to get the intersection of points
            System.out.println("Intersection: " + goldPoints);
            // Hur många gemensamma segmenteringar har guld och pred?
            int correct = goldPoints.size();
            // Om vi har minst en rätt
            if(correct > bestCorrect || (correct == bestCorrect && goldSize < bestTotal)) {
                System.out.println("Updated best");
                bestCorrect = correct;
                bestTotal = goldSize;
            }
            // Fattar inte alls.
            //      Är antalet segment i guldsegmenteringen mindre än 100 (borde i praktiken alltid vara sant)?
            if(goldSize < minBestTotal)
                minBestTotal = goldSize;
        }
        // Om pred och guld har 0 gemensamma segment
        // I praktiken innebär det här att bestTotal alltid är lika med goldSize, eftersom minBestTotal alltid sätts till goldSize (om goldSize < 100)
        if(bestTotal == 0)
            bestTotal = minBestTotal;

        // bestCorrect är maximala antalet gemensamma segment
        // bestTotal är goldSize, vet inte vad den faktiskt ska återspegla
        // predSize är predSize
        double[] ret = new double[]{bestCorrect, bestTotal, predSize};
        System.out.println("Returning: " + ret[0] + ", " + ret[1] + ", " + ret[2]);
        return ret;
    }


    static double evaluateSegmentation() {
        //uses static variables from the Model class directly
        MorphoChain.TEST = true;
        double correct = 0., predTotal =0., goldTotal =0.;

        System.out.println("Evaluating segmentations...");
        predictedSegmentations.clear();
        incorrectSegmentations.clear();
        correctSegmentations.clear();

        for(Pair<String, ArrayList<String>> entry : MorphoChain.goldSegs) {
            //segment without explicit chain
            String predSeg = MorphoChain.segment(entry.getKey());

            double [] retValues = evaluateSegmentationPoints(predSeg, entry.getValue());

            correct += retValues[0];goldTotal += retValues[1];predTotal += retValues[2];
            predictedSegmentations.put(entry.getKey(), predSeg);
            // Problemet var att den kollade antalet predicted jämfört med antalet gold
            // Det finns två möjliga tolkningar av vad den borde visa här:
            //  1. Alla segmenteringar som är helt korrekta (vad jag ändrade till)
            //  2. Alla segmenteringar som har minst ett korrekt segment
            // Det är inte den här listan vi bryr oss om i slutändan ändå,
            // utan det är precision/recall/F1 som är det viktiga, och de är korrekta
            if(retValues[0] != retValues[1])
                incorrectSegmentations.put(entry.getKey(), predSeg+" : "+entry.getValue());
            else {
                correctSegmentations.put(entry.getKey(), predSeg+" : "+entry.getValue());
            }

        }
        System.out.println("Done.");
        System.out.println("\nIncorrect Segmentations (" + incorrectSegmentations.size() + "):");
        printSegmentations(incorrectSegmentations);
        System.out.println("\nCorrect Segmentations (" + correctSegmentations.size() + "):");
        printSegmentations(correctSegmentations);

        System.out.println("\nAll Segmentations (" + predictedSegmentations.size() + "):");
        printSegmentations(predictedSegmentations);

        // Ser rätt ut
        double precision = correct/predTotal, recall = correct/goldTotal;
        // Ser rätt ut
        double f1 = (2*precision*recall)/(precision+recall);
        // Correct här är alltså antalet uppdelningar, inte antalet helt korrekta segmenteringar
        // Så om vi har pred: gift-e-rmål och gold: gift-er-mål så har vi 1 korrekt segmentering t-e
        System.out.println("Correct: "+correct+" GoldTotal: " + goldTotal+ " PredTotal: "+predTotal);
        System.out.println("Precision: " + precision + " Recall: " + recall + " F1: " + f1);
        MorphoChain.TEST = false;
        return f1;
    }


    private static void printSegmentations(HashMap<String, String> segmentations) {
        //print the segmentations
        for(Map.Entry<String, String> entry : segmentations.entrySet())
            System.out.println(entry.getKey()+" # "+entry.getValue());
    }
}
