package uk.ac.york.mocha.simulator.experiments_CARVB;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.generator.CacheHierarchy;
import uk.ac.york.mocha.simulator.generator.SystemGenerator;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.Allocation;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.Hardware;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.RecencyType;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.SimuType;
import uk.ac.york.mocha.simulator.simulator.SimualtorNWC;
import uk.ac.york.mocha.simulator.simulator.Utils;

/* Number, Type, Effect */

/*
 * Show the climb effects, can we model that as pressure, i.e. if the pressure
 * is higher than the threshold then it will impact the makespan?
 */

public class CorrelationCoefficient {

	static DecimalFormat df = new DecimalFormat("#.###");

	public static enum faultType {
		all_nodes, all_critical, all_non_critical, high_et, high_pathNum, high_pathET, sensivitiy, high_in_degree,
		high_out_degree, high_in_out_degree, statSensitivity
	}

	static int nos = 100;
	static int[] allCores = { 4 };
	static boolean print = false;
	static double[] allPercent = { 0.1, 0.2, 0.3, 0.4, 0.5 };
//	static double[] allEffect = { 0.1, 0.2, 0.3, 0.4, 0.5 };
	static List<Double> allEffect;
	static int[] allInstanceNum = { 1, 3, 5, 10 };

	static Random rng = new Random(1000);

	static int nop = 4;

	public static void main(String args[]) {

		start(nop);
	}

	public static void start(int nop) {

		for (int i = 0; i < allPercent.length; i++) {
			String folderName = "result/" + "faults_new/";
			String fileName = "/cc" + "_" + allCores[0] + "_" + allPercent[i] + "_" + allInstanceNum[0] + ".txt";
			Utils.writeResult(folderName, fileName, "", false);
		}

		for (int i = 0; i < allPercent.length; i++) {
			faults(allCores[0], allPercent[i], allInstanceNum[0], nop);
		}

	}

	public static synchronized void addAll(List<List<Pair<Double, Long>>> res, List<List<Pair<Double, Long>>> add) {
		res.addAll(add);
	}

	public static void faults(int cores, double percent, int instanceNum, int nop) {

		List<List<Pair<Double, Long>>> allResult = new ArrayList<>();

		List<Thread> runners = new ArrayList<>();

		for (int i = 0; i < nop; i++) {

			final int id = i;
			final int workload = (int) Math.ceil((double) nos / (double) nop);

			runners.add(new Thread(new Runnable() {

				@Override
				public void run() {

					List<List<Pair<Double, Long>>> result = runOneThread(null, cores, percent, instanceNum, workload,
							id);
					addAll(allResult, result);
				}
			}));
		}

		for (Thread t : runners)
			t.start();

		for (Thread t : runners)
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

	}

	public static List<List<Pair<Double, Long>>> runOneThread(Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys,
			int cores, double percent, int instanceNum, int workload, int id) {

		List<List<Pair<Double, Long>>> res = new ArrayList<>();

		for (int i = 0; i < workload; i++) {

			System.out.println("No. of system: " + (i + id * workload) + " --- " + "cores: " + cores + ", percent: "
					+ percent + ", No. instance: " + instanceNum);

			for (int j = 1; j <= 50001; j += 500) {

//				rng = new Random(1000);
				SystemGenerator gen = new SystemGenerator(SystemParameters.coreNum, 1, true, true, null, rng, true,
						print);
				sys = gen.generatedDAGInstancesInOneHP(instanceNum, -1, null, false);

				int k = j;

				List<Pair<Double, Long>> results = new ArrayList<>();

				results.addAll(run(sys, cores, faultType.high_et, percent, k, print));

				results.addAll(run(sys, cores, faultType.high_pathET, percent, k, print));

				results.addAll(run(sys, cores, faultType.high_in_degree, percent, k, print));

				results.addAll(run(sys, cores, faultType.high_out_degree, percent, k, print));

				results.addAll(run(sys, cores, faultType.high_in_out_degree, percent, k, print));

				results.addAll(run(sys, cores, faultType.high_pathNum, percent, k, print));

				writeToSystem(results, cores, percent, instanceNum, true);
			}
		}

		return res;
	}

	public synchronized static void writeToSystem(List<Pair<Double, Long>> ll, int cores, double percent, int instanceNum,
			boolean append) {

		String out = "";

		for (int k = 0; k < ll.size(); k++) {
//				System.out.print(ll.get(k) + " ");
			out += ll.get(k).getFirst() + " " + ll.get(k).getSecond();
			if (k != ll.size() - 1)
				out += " ";
		}
//			System.out.println();
		out += "\n";

		String folderName = "result/" + "faults_new/";
		String fileName = "/cc" + "_" + cores + "_" + percent + "_" + instanceNum + ".txt";
		Utils.writeResult(folderName, fileName, out, append);
	}

	public static List<Pair<Double, Long>> run(Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys, int cores,
			faultType type, double percent, long id, boolean print) {

		List<Pair<Double, Long>> makespans = new ArrayList<>();

		if (print)
			System.out.println("\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ " + type.toString()
					+ "$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
		setUpSpecificFaults(sys.getFirst(), type, percent, false, print);
		Pair<Double, Long> makespan = oneRun(sys, cores, id, print);
		makespans.add(makespan);
		if (print)
			System.out.println(
					"$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$\n");

		return makespans;
	}

	public static List<List<Node>> setUpSpecificFaults(List<DirectedAcyclicGraph> dags, faultType type, double percent,
			boolean opposite, boolean print) {

		List<List<Node>> faultNodesinDAGs = new ArrayList<>();

		for (DirectedAcyclicGraph d : dags)
			for (Node n : d.getFlatNodes())
				n.hasFaults = false;

		for (DirectedAcyclicGraph d : dags) {

			// all nodes with no order
			List<Node> allNodes = new ArrayList<Node>(d.getFlatNodes());

			List<Node> faultNodes = new ArrayList<>();
			int faultNodeNum = 0;

			switch (type) {
			case all_nodes:
				if (!opposite)
					faultNodes = new ArrayList<>(allNodes);
				else
					faultNodes = new ArrayList<>();
				break;

			case high_et:
				allNodes.sort((c1, c2) -> compareNodes(dags, c1, c2, opposite, type));
				faultNodeNum = (int) Math.ceil(percent * (double) allNodes.size());

				for (int i = 0; i < faultNodeNum; i++) {
					faultNodes.add(allNodes.get(i));
				}
				break;

			case high_pathNum:
				allNodes.sort((c1, c2) -> compareNodes(dags, c1, c2, opposite, type));
				faultNodeNum = (int) Math.ceil(percent * (double) allNodes.size());

				for (int i = 0; i < faultNodeNum; i++) {
					faultNodes.add(allNodes.get(i));
				}
				break;

			case high_pathET:
				allNodes.sort((c1, c2) -> compareNodes(dags, c1, c2, opposite, type));
				faultNodeNum = (int) Math.ceil(percent * (double) allNodes.size());

				for (int i = 0; i < faultNodeNum; i++) {
					faultNodes.add(allNodes.get(i));
				}
				break;

			case high_out_degree:
				allNodes.sort((c1, c2) -> compareNodes(dags, c1, c2, opposite, type));

				faultNodeNum = (int) Math.ceil(percent * (double) allNodes.size());

				for (int i = 0; i < faultNodeNum; i++) {
					faultNodes.add(allNodes.get(i));
				}
				break;

			case high_in_degree:
				allNodes.sort((c1, c2) -> compareNodes(dags, c1, c2, opposite, type));

				faultNodeNum = (int) Math.ceil(percent * (double) allNodes.size());

				for (int i = 0; i < faultNodeNum; i++) {
					faultNodes.add(allNodes.get(i));
				}
				break;

			case high_in_out_degree:
				allNodes.sort((c1, c2) -> compareNodes(dags, c1, c2, opposite, type));

				faultNodeNum = (int) Math.ceil(percent * (double) allNodes.size());

				for (int i = 0; i < faultNodeNum; i++) {
					faultNodes.add(allNodes.get(i));
				}
				break;

			case statSensitivity:
				allNodes.sort((c1, c2) -> compareNodes(dags, c1, c2, opposite, type));
				faultNodeNum = (int) Math.ceil(percent * (double) allNodes.size());

				for (int i = 0; i < faultNodeNum; i++) {
					faultNodes.add(allNodes.get(i));
				}
				break;
			case sensivitiy:

				/*
				 * Here we normalise the three major factors into one.
				 */
				allNodes.sort((c1, c2) -> compareNodes(dags, c1, c2, opposite, type));
				faultNodeNum = (int) Math.ceil(percent * (double) allNodes.size());

				for (int i = 0; i < faultNodeNum; i++) {
					faultNodes.add(allNodes.get(i));
				}

				break;

			default:
				break;
			}

			if (print)
				System.out.println("Fault nodes: ");

			for (Node n : faultNodes) {
				n.hasFaults = true;
				n.cvp.median = 0;
				n.cvp.range = -1; // ((double) rng.nextInt(effect +
				// 1) / (double) 100) / 3.0;
				if (print)
					System.out.println(n.toString() + ": " + n.cvp.median + ", " + n.cvp.range);
			}

			faultNodesinDAGs.add(faultNodes);

		}

		return faultNodesinDAGs;
	}

	public static Pair<Double, Long> oneRun(Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys, int cores, long id,
			boolean print) {
		SimualtorNWC no_fault = new SimualtorNWC(SimuType.CLOCK_LEVEL, Hardware.PROC_CACHE,
				Allocation.CACHE_AWARE_ROBUST_v2_2, RecencyType.TIME_DEFAULT, sys.getFirst(), sys.getSecond(), cores, 0,
				id, true);
		no_fault.simulate(print);

		List<DirectedAcyclicGraph> dags = sys.getFirst();
		if (print)
			System.out.println(dags.get(dags.size() - 1).finishTime - dags.get(dags.size() - 1).startTime);

		long makespan = dags.get(dags.size() - 1).finishTime - dags.get(dags.size() - 1).startTime;
//		long sumET = dags.get(dags.size() - 1).variation;
		double sumET = dags.get(dags.size() - 1).getFlatNodes().stream().mapToDouble(c -> c.variation).sum();
		return new Pair<Double, Long>(sumET, makespan);
	}

	public static int compareNodes(List<DirectedAcyclicGraph> dags, Node c1, Node c2, boolean oppsite, faultType type) {
		switch (type) {

		case high_et:
			return compareNodebyET(c1, c2, oppsite);

		case high_pathNum:
			return compareNodebyPath(dags, c1, c2, oppsite);

		case high_pathET:
			return compareNodebyPathET(dags, c1, c2, oppsite);

		case high_out_degree:
			return compareNodebyOutDegree(c1, c2, oppsite);

		case high_in_degree:
			return compareNodebyInDegree(c1, c2, oppsite);

		case high_in_out_degree:
			return compareNodebyInAndOutDegree(c1, c2, oppsite);

		case sensivitiy:
			return compareNodebySensitivity(dags, c1, c2, oppsite);

		default:
			System.err.println("Line 416: Unkown type in method compareNodes(), type: " + type.toString());
			System.exit(-1);
			break;
		}

		return 0;
	}

	public static int compareNodebySensitivity(List<DirectedAcyclicGraph> dags, Node c1, Node c2, boolean oppsite) {
		if (oppsite)
			return Double.compare(c1.sensitivity, c2.sensitivity);
		else
			return -Double.compare(c1.sensitivity, c2.sensitivity);
	}

	public static int compareNodebyPathET(List<DirectedAcyclicGraph> dags, Node c1, Node c2, boolean oppsite) {
		if (oppsite)
			return Long.compare(c1.pathET, c2.pathET);
		else
			return -Long.compare(c1.pathET, c2.pathET);
	}

	public static int compareNodebyPath(List<DirectedAcyclicGraph> dags, Node c1, Node c2, boolean oppsite) {
		if (oppsite)
			return Long.compare(c1.pathNum, c2.pathNum);
		else
			return -Long.compare(c1.pathNum, c2.pathNum);
	}

	public static int compareNodebyET(Node c1, Node c2, boolean oppsite) {
		if (oppsite)
			return Long.compare(c1.getWCET(), c2.getWCET());
		else
			return -Long.compare(c1.getWCET(), c2.getWCET());
	}

	public static int compareNodebyOutDegree(Node c1, Node c2, boolean oppsite) {
		if (oppsite)
			return Integer.compare(c1.getChildren().size(), c2.getChildren().size());
		else
			return -Integer.compare(c1.getChildren().size(), c2.getChildren().size());
	}

	public static int compareNodebyInDegree(Node c1, Node c2, boolean oppsite) {
		if (oppsite)
			return Integer.compare(c1.getParent().size(), c2.getParent().size());
		else
			return -Integer.compare(c1.getParent().size(), c2.getParent().size());
	}

	public static int compareNodebyInAndOutDegree(Node c1, Node c2, boolean oppsite) {
		if (oppsite)
			return Integer.compare(c1.getParent().size() + c1.getChildren().size(),
					c2.getParent().size() + c2.getChildren().size());
		else
			return -Integer.compare(c1.getParent().size() + c1.getChildren().size(),
					c2.getParent().size() + c2.getChildren().size());
	}

}
