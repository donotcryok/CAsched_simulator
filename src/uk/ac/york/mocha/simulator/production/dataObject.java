package uk.ac.york.mocha.simulator.production;

import java.util.List;

public class dataObject {

    public List<Long> IntegrandVariable;
    public List<List<Long>> Item;
    public List<List<Integer>> Item1;
    public List<List<Integer>> Item2;

    public dataObject(List<Long> IntegrandVariable, List<List<Long>> Item, List<List<Integer>> Item1,
            List<List<Integer>> Item2) {
        this.IntegrandVariable = IntegrandVariable;
        this.Item = Item;
        this.Item1 = Item1;
        this.Item2 = Item2;
    }
}
