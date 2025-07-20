package uk.ac.york.mocha.simulator.allocation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.util.Pair;

import it.unimi.dsi.fastutil.Arrays;
import jnr.ffi.Struct.LONG;
import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;
import uk.ac.york.mocha.simulator.simulator.Utils;

public class OnlineCARVB_SEEN extends AllocationMethods {
	public static List<List<Long>> etHistOneNode = new ArrayList<>();

	@Override
	public void allocate(List<DirectedAcyclicGraph> dags, List<Node> readyNodes, List<List<Node>> localRunqueue,
			List<Integer> cores, long[] availableTimeAllProcs, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3, List<List<Node>> allocHistory, long currentTime,
			boolean lcif, List<Node> etHist) {

		List<Integer> availableCores = new ArrayList<>();
		for (int i = 0; i < cores.size(); i++) {
			if (localRunqueue.get(i).size() == 0 && availableTimeAllProcs[i] <= currentTime)
				availableCores.add(i);
		}

		if (readyNodes.size() == 0 || availableCores.size() == 0)
			return;

		readyNodes.stream().forEach(c -> c.partition = -1);

		readyNodes.sort((c1, c2) -> Utils.compareNodeByPriorityAndSensitivity(dags, c1, c2));

		List<Node> preEligible = new ArrayList<>();
		for (int i = 0; i < availableCores.size(); i++) {
			if (readyNodes.size() == i)
				break;
			preEligible.add(readyNodes.get(i));
		}

		List<Integer> availableP = new ArrayList<>(availableCores);

		List<List<Long>> speedUpTable = new ArrayList<>();
		List<List<Long>> cacheTable = new ArrayList<>();

		for (Node n : preEligible) {
			List<Long> ETdrop = new ArrayList<>();
			List<Long> cachedrop = new ArrayList<>();

			for (int i = 0; i < history_level1.size(); i++) {
				int proc = i;
				if (availableP.contains(proc)) {
					/*
					 * Speed up by ABSOLUTE value
					 */
					long WCET = n.getWCET();

					Pair<Pair<Long, Double>, Integer> ETWithCache = n.crp.computeET(-1, history_level1, history_level2,
							history_level3, n, proc, true, 0, 0, false);
					long realET = ETWithCache.getFirst().getFirst();
					long speedup = WCET - realET;

					ETdrop.add(speedup);

					cachedrop.add((long) ETWithCache.getSecond());
				}
			}

			speedUpTable.add(ETdrop);
			cacheTable.add(cachedrop);
		}

		List<Integer> allocProcs = new ArrayList<>();
		List<Integer> allocNodes = new ArrayList<>();

		List<List<Node>> historyCut = new ArrayList<>();
		for (int i = 0; i < history_level1.size(); i++) {
			if (availableP.contains(i))
				historyCut.add(history_level1.get(i));
		}

		List<List<Node>> allocHistoryCut = new ArrayList<>();
		for (int i = 0; i < allocHistory.size(); i++) {
			if (availableP.contains(i))
				allocHistoryCut.add(allocHistory.get(i));
		}

		for (int k = 0; k < availableP.size(); k++) {
			if (k >= preEligible.size())
				break;

			Pair<Integer, Integer> p = setPartition(speedUpTable, cacheTable, allocNodes, allocProcs, allocHistoryCut,
					allocHistory, preEligible, availableP, availableTimeAllProcs, currentTime, lcif, history_level1,
					history_level2, history_level3, etHist);

			Node n = preEligible.get(p.getFirst().intValue());

			n.partition = availableP.get(p.getSecond().intValue());

			allocNodes.add(p.getFirst().intValue());
			allocProcs.add(p.getSecond().intValue());

			localRunqueue.get(n.partition).add(n);
			allocHistory.get(n.partition).add(n);
		}

		for (int i = 0; i < readyNodes.size(); i++) {
			if (readyNodes.get(i).partition > -1) {
				readyNodes.remove(i);
				i--;
			}
		}

	}

	private Pair<Integer, Integer> setPartition(List<List<Long>> speedUpTable, List<List<Long>> cacheTable,
			List<Integer> allocNodes, List<Integer> allocProcs, List<List<Node>> allocHistory,
			List<List<Node>> fullAllocHistory, List<Node> preEligible, List<Integer> procs,
			long[] availableTimeAllProcs, long time, boolean lcif, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3, List<Node> etHist) {

		int row = -1;
		int col = -1;
		long chosen_speedUp = -1;

		for (int i = 0; i < speedUpTable.size(); i++) {
			if (!allocNodes.contains(i)) {

				long minDiff = Long.MAX_VALUE;
				long standardET = 0;
				long standardCache = 0;

				Node n = preEligible.get(i);

				if (SystemParameters.m == 3) {
					long historyET = 0;
					// 第一轮取最小值
					if (OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).size() == 0) {
						historyET = Long.MAX_VALUE;
						for (int j = 0; j < speedUpTable.get(i).size(); j++) {
							if (!allocProcs.contains(j)) {
								long expectedET = n.getWCET() - speedUpTable.get(i).get(j);
								if (expectedET < historyET) {
									historyET = expectedET;
									chosen_speedUp = speedUpTable.get(i).get(j);
									row = i;
									col = j;

								}
							}
						}
						OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).add(historyET);
					} else {
						historyET = OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).getFirst();
						long mint2 = Long.MAX_VALUE;// 当前可加速的最大值
						int minj = -1;
						for (int j = 0; j < speedUpTable.get(i).size(); j++) {
							if (!allocProcs.contains(j)) {
								long expectedET = n.getWCET() - speedUpTable.get(i).get(j);
								if (expectedET < mint2) {
									mint2 = expectedET;
									minj = j;
								}
								// 寻找是否有更大的加速，有就上升
								if (expectedET <= historyET) {
									if (minDiff > Math.abs((historyET) - expectedET)) {
										minDiff = Math.abs((historyET) - expectedET);
										chosen_speedUp = speedUpTable.get(i).get(j);
										row = i;
										col = j;
										OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).set(0, expectedET);
									}
								}
							}

						}
						// 如果没有更大的加速，就下降
						if (mint2 > historyET) {
							historyET = historyET + (mint2 - historyET) / 2;
							OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).set(0, historyET);
							row = i;
							col = minj;
						}
					}

				} else if (SystemParameters.m < 3) {
					List<Long> valuesETlLongs = OnlineCARVB_SEEN.etHistOneNode.get(n.getId());
					double[] valuesET = new double[valuesETlLongs.size()];
					for (int k = 0; k < valuesETlLongs.size(); k++) {
						valuesET[k] = valuesETlLongs.get(k) != null ? valuesETlLongs.get(k) : 0.0; // 处理 null 元素
					}

					if (SystemParameters.m == 0) {
						// 中位数
						Median med = new Median();
						standardET = (long) Math.round(med.evaluate(valuesET));
						// standardCache = (long) Math.round(med.evaluate(valuesCache));

					} else if (SystemParameters.m == 1) {
						// 25%分位数
						Percentile percentile = new Percentile(25);
						standardET = (long) Math.round(percentile.evaluate(valuesET));
						// standardCache = (long) Math.round(percentile.evaluate(valuesCache));
					} else if (SystemParameters.m == 2) {
						// //最小值
						if (valuesET.length > 0) {
							double mint = valuesET[0];
							for (int j = 1; j < valuesET.length; j++) {
								if (valuesET[j] < mint) {
									mint = valuesET[j]; // 更新最小值
								}
							}
							standardET = (long) mint;
						}
					}

					for (int j = 0; j < speedUpTable.get(i).size(); j++) {
						if (!allocProcs.contains(j)) {
							long expectedET = n.getWCET() - speedUpTable.get(i).get(j);
							if (expectedET < n.getWCET() * 0.8) {
								OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).add(expectedET);
							}
							if (minDiff > Math.abs((standardET) - expectedET)) {
								minDiff = Math.abs((standardET) - expectedET);
								chosen_speedUp = speedUpTable.get(i).get(j);
								row = i;
								col = j;
							}
						}
					}

				} else {
					if (n.sensitivityL > 0) {

						if (OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).size() == 2) {
							Integer r = OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).getFirst().intValue();
							Integer c = OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).getLast().intValue();
							if (!allocProcs.contains(c)) {
								return new Pair<Integer, Integer>(i, c);
							}
						}
						standardET = 0;// MSF

					} else {
						// standardET = 0;// MSF
						// MCF
						List<long[]> etHistOneNode = Utils.getETHistroy(n, etHist);
						double[] valuesET = new double[etHistOneNode.size()];
						double[] valuesCache = new double[etHistOneNode.size()];
						for (int k = 0; k < etHistOneNode.size(); k++) {
							valuesET[k] = etHistOneNode.get(k)[0];
							valuesCache[k] = etHistOneNode.get(k)[1];
						}
						Median med = new Median();
						standardET = (long) Math.round(med.evaluate(valuesET));
						standardCache = (long) Math.round(med.evaluate(valuesCache));
					}
					for (int j = 0; j < speedUpTable.get(i).size(); j++) {
						if (!allocProcs.contains(j)) {
							long expectedET = n.getWCET() - speedUpTable.get(i).get(j);

							if (minDiff > Math.abs((standardET) - expectedET)) {
								minDiff = Math.abs((standardET) - expectedET);
								chosen_speedUp = speedUpTable.get(i).get(j);
								row = i;
								col = j;
							}
						}
					}
					if (OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).size() < 2) {
						OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).add(Long.valueOf(row));
						OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).add(Long.valueOf(col));

					} else {
						OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).set(0, Long.valueOf(row));
						OnlineCARVB_SEEN.etHistOneNode.get(n.getId()).set(1, Long.valueOf(col));
					}

				}

				break;
			}
		}

		if (row == -1 || col == -1) {
			System.err.println("SimpleCacheAware.getIndexOfMaximum(): Cannot find the max value!");

			System.exit(-1);
		}

		return new Pair<Integer, Integer>(row, col);
	}

}