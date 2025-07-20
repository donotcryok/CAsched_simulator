package uk.ac.york.mocha.simulator.experiments_CARVB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.IntStream;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.util.Pair;
import org.python.antlr.ast.Print;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.DoubleAdder;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.york.mocha.simulator.allocation.OnlineCARVB;
import uk.ac.york.mocha.simulator.allocation.OnlineCARVB_SEEN;
import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.generator.CacheHierarchy;
import uk.ac.york.mocha.simulator.generator.SystemGenerator;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.Allocation;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.ExpName;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.Hardware;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.RecencyType;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.SimuType;
import uk.ac.york.mocha.simulator.production.dataObject;
import uk.ac.york.mocha.simulator.resultAnalyzer.AllSystemsResults;
import uk.ac.york.mocha.simulator.resultAnalyzer.OneSystemResults;
import uk.ac.york.mocha.simulator.simulator.Simualtor;
import uk.ac.york.mocha.simulator.simulator.SimualtorNWC;
import uk.ac.york.mocha.simulator.simulator.Utils;
import uk.ac.york.mocha.simulator.production.*;

public class CARVB_General {

	static DecimalFormat df = new DecimalFormat("#.###");

	static int cores = 4;
	static int nos = 5; // number of system
	static int intanceNum = 100;

	static int startUtil = 4;
	static int incrementUtil = 4;
	static int endUtil = 36;

	static boolean print = false;

	public static List<Double> IntegrandVariable = new ArrayList<>();
	public static List<List<Double>> Item = new ArrayList<>();
	public static List<List<Integer>> Item1 = new ArrayList<>();
	public static List<List<Integer>> Item2 = new ArrayList<>();

	public static void main(String args[]) {
		oneTaskWithFaults();
	}

	public static void oneTaskWithFaults() {
		int hyperPeriodNum = -1;
		int seed = 1000;

		for (int i = startUtil; i <= endUtil; i = i + incrementUtil) {
			SystemParameters.utilPerTask = Double.parseDouble(df.format((double) i / (double) 10));
			RunOneGroup(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.predict);
		}
	}

	static boolean bigger = false;

	public static void RunOneGroup(int taskNum, int intanceNum, int hyperperiodNum, boolean takeAllUtil,
			List<List<Double>> util, int taskSeed, int tableSeed, List<List<Long>> periods, int NoS, boolean randomC,
			ExpName name) {

		int[] instanceNo = new int[taskNum];

		if (periods != null && hyperperiodNum > 0) {
			long totalHP = Utils.getHyperPeriod(periods.get(0)) * hyperperiodNum;

			for (int i = 0; i < periods.size(); i++) {
				int insNo = (int) (totalHP / periods.get(0).get(i));
				instanceNo[i] = insNo > intanceNum ? insNo : intanceNum;
			}
		} else if (intanceNum > 0) {
			for (int i = 0; i < instanceNo.length; i++)
				instanceNo[i] = intanceNum;
		} else {
			System.out.println("Cannot get same instances number for randomly generated periods.");
		}

		List<OneSystemResults> allRes = new ArrayList<>();

		for (int i = 0; i < nos; i++) {
			System.out.println(
					"Util per task: " + SystemParameters.utilPerTask + " --- Current system number: " + (i + 1));

			SystemGenerator gen = new SystemGenerator(cores, 1, true, true, null, taskSeed + i, true, print);
			Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(intanceNum, -1,
					null, false);

			OneSystemResults res = null;
			res = testOneCaseThreeMethod(sys, taskNum, instanceNo, cores, taskSeed, tableSeed, i);

			allRes.add(res);
			taskSeed++;
		}
		new AllSystemsResults(allRes, instanceNo, cores, taskNum, name);
	}

	// public static List<Double>
	// CalculateSensitivity(Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys) {
	// // system 里面的dag都是一样的
	// List<List<Node>> allpaths = sys.getFirst().getFirst().allpaths;
	// List<List<Node>> Candidates = new ArrayList<>();
	// List<Node> allnode = sys.getFirst().getFirst().getFlatNodes();
	// int nodesize = allnode.size();
	// List<Long> IdtoWCET = new ArrayList<>(Collections.nCopies(nodesize, 0L));

	// for (Node n : allnode) {
	// IdtoWCET.set(n.getId(), n.WCET);
	// }

	// Long maxlowerbound = (long) 0;
	// Long[] upperboundList = new Long[allpaths.size()];

	// for (int i = 0; i < allpaths.size(); i++) {
	// Long temp = (long) 0;
	// for (Node n : allpaths.get(i)) {
	// temp += n.WCET;
	// }
	// upperboundList[i] = temp;
	// temp = (long) (temp * 0.8); // 这里设下界就是上界的0.3
	// maxlowerbound = temp > maxlowerbound ? temp : maxlowerbound;
	// }
	// // 假设是正态分布<均值，方差>，方差用上下界决定
	// // List<Pair<Integer, Integer>> distributedList = new ArrayList<>();
	// for (int i = 0; i < allpaths.size(); i++) {
	// if (upperboundList[i] > maxlowerbound) {
	// Candidates.add(allpaths.get(i));
	// }
	// }

	// List<Integer> sharenode = new ArrayList<>(Collections.nCopies(nodesize, 0));
	// // 在候选路径里面找共享节点！！！！
	// for (int i = 0; i < Candidates.size(); i++) {
	// for (Node n : Candidates.get(i)) {
	// sharenode.set(n.getId(), sharenode.get(n.getId()) + 1); // 大于1的是被共享的节点
	// }
	// }

	// // 开始做json
	// List<Object> allData = new ArrayList<>();
	// for (int i = 0; i < Candidates.size(); i++) {
	// List<Long> IntegrandVariable = new ArrayList<>();
	// List<List<Long>> Item = new ArrayList<>();
	// List<List<Integer>> Item1 = new ArrayList<>();
	// List<List<Integer>> Item2 = new ArrayList<>();

	// List<Integer> idtoid = new ArrayList<>(Collections.nCopies(nodesize, -1));
	// List<Integer> p = new ArrayList<>();
	// // Long temp = 0L;
	// for (Node n : Candidates.get(i)) {
	// p.add(n.getId());
	// // 不和共享节点重复添加
	// if (sharenode.get(n.getId()) == 1) {
	// // temp += IdtoWCET.get(n.getId());
	// // idtoid.set(n.getId(), 0);
	// IntegrandVariable.add(IdtoWCET.get(n.getId()));
	// idtoid.set(n.getId(), IntegrandVariable.size() - 1);
	// }
	// }
	// // IntegrandVariable.add(temp);
	// for (int k = 0; k < sharenode.size(); k++) {
	// // 且节点没有被所有候选路径共享
	// if (sharenode.get(k) > 1 && sharenode.get(k) < Candidates.size()) {
	// IntegrandVariable.add(IdtoWCET.get(k));
	// idtoid.set(k, IntegrandVariable.size() - 1);
	// }
	// }

	// for (int j = 0; j < Candidates.size(); j++) {
	// List<Long> item = new ArrayList<>();
	// List<Integer> item1 = new ArrayList<>();
	// List<Integer> item2 = new ArrayList<>();
	// if (i == j) {
	// continue;
	// }
	// List<Integer> p2 = new ArrayList<>();
	// for (Node n : Candidates.get(j)) {
	// if (!p.contains(n.getId())) {
	// if (sharenode.get(n.getId()) == 1) {
	// item.add(IdtoWCET.get(n.getId()));
	// } else if (sharenode.get(n.getId()) > 1) {
	// item2.add(idtoid.get(n.getId()));
	// }
	// }
	// p2.add(n.getId());
	// }
	// for (Integer id : p) {
	// if (!p2.contains(id)) {
	// item1.add(idtoid.get(id));
	// }
	// }
	// Item.add(item);
	// Item1.add(item1);
	// Item2.add(item2);
	// }

	// allData.add(new Object[] { IntegrandVariable, Item, Item1, Item2 });

	// }
	// // 创建 ObjectMapper 实例
	// ObjectMapper objectMapper = new ObjectMapper();

	// // 将对象写入到文件中
	// File file = new File("data.json");
	// try {
	// objectMapper.writeValue(file, allData);
	// } catch (StreamWriteException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (DatabindException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }

	// try {
	// // Python脚本路径
	// String pythonScriptPath = "Integran.py";
	// // 创建ProcessBuilder对象，指定命令
	// ProcessBuilder processBuilder = new ProcessBuilder("python",
	// pythonScriptPath);
	// // 启动Python脚本
	// Process process = processBuilder.start();
	// // 等待Python脚本执行完毕
	// int exitCode = process.waitFor();
	// System.out.println("Python script executed with exit code: " + exitCode);

	// } catch (IOException | InterruptedException e) {
	// e.printStackTrace();
	// }

	// List<Double> ProbabilityList = new ArrayList<>();
	// try {
	// ProbabilityList = objectMapper.readValue(file, List.class);
	// } catch (StreamReadException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (DatabindException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }

	// List<Double> sensitivitylList = new ArrayList<>();
	// for (int i = 0; i < sys.getFirst().getFirst().getFlatNodes().size(); i++) {
	// sensitivitylList.add(0.0);
	// }
	// // 敏感度计算 在候选路径的上的节点先加1分
	// // for (int i = 0; i < Candidates.size(); i++) {
	// // for (Node n : Candidates.get(i)) {
	// // if(sensitivitylList.get(n.getId())==0){
	// // sensitivitylList.set(n.getId(), 1.0);
	// // }
	// // }
	// // }

	// for (int i = 0; i < Candidates.size(); i++) {
	// for (Node n : Candidates.get(i)) {
	// sensitivitylList.set(n.getId(), sensitivitylList.get(n.getId()) +
	// ProbabilityList.get(i));
	// }
	// }

	// return sensitivitylList;
	// }

	public static double function(double[] args) {
		double Y = 1;

		for (int i = 0; i < Item.size(); i++) {
			List<Double> x = Item.get(i);
			List<Integer> x1 = Item1.get(i);
			List<Integer> x2 = Item2.get(i);

			double sumResult = 0;
			for (int idx : x1) {
				sumResult += args[idx];
			}
			for (int idx : x2) {
				sumResult -= args[idx];
			}

			if (x.size() > 0) {
				// Compute the mean and standard deviation for normal distribution
				double mean = 0;
				for (double val : x) {
					mean += val;
				}
				mean = mean * (1 + 1.0 / 3.0) / 2.0;

				double std2 = 0;
				for (double val : x) {
					std2 += Math.pow(val * (1 - 1.0 / 3.0) / (2.0 * 3.0), 2);
				}

				NormalDistribution norm = new NormalDistribution(mean, Math.sqrt(std2));
				Y *= norm.cumulativeProbability(sumResult);
			} else {
				Y *= sumResult > 0 ? 1 : 0;
			}
		}

		for (int i = 0; i < args.length; i++) {
			double WCET = IntegrandVariable.get(i);
			NormalDistribution norm = new NormalDistribution(WCET * (1 + 1.0 / 3.0) / 2.0,
					WCET * (1 - 1.0 / 3.0) / (2.0 * 3.0));
			Y *= norm.density(args[i]);
		}

		return Y;
	}

	// 蒙特卡罗方法进行任意维度积分
	public static double monteCarloIntegration(int numPoints, double[] minValues, double[] maxValues) {
		Random random = new Random();
		int dimension = minValues.length;
		double sum = 0;
		double volume = 1;

		// 计算积分区域的体积
		for (int i = 0; i < dimension; i++) {
			volume *= (maxValues[i] - minValues[i]);
		}

		// 生成随机点并计算函数值
		for (int i = 0; i < numPoints; i++) {
			double[] randomPoint = new double[dimension];
			for (int j = 0; j < dimension; j++) {
				randomPoint[j] = minValues[j] + (maxValues[j] - minValues[j]) * random.nextDouble();
			}
			sum += function(randomPoint);
		}

		// 计算积分值
		return volume * sum / numPoints;
	}

	public static double parallelMonteCarloIntegration(int numPoints, double[] minValues, double[] maxValues) {
		int dimension = minValues.length;
		double volume = 1;

		// 计算积分区域的体积
		for (int i = 0; i < dimension; i++) {
			volume *= (maxValues[i] - minValues[i]);
		}

		DoubleAdder sumAdder = new DoubleAdder();

		// 使用并行流处理随机点
		IntStream.range(0, numPoints)
				.parallel()
				.forEach(i -> {
					double[] randomPoint = new double[dimension];
					ThreadLocalRandom random = ThreadLocalRandom.current();

					// 生成随机点
					for (int j = 0; j < dimension; j++) {
						randomPoint[j] = minValues[j] + (maxValues[j] - minValues[j]) * random.nextDouble();
					}

					sumAdder.add(function(randomPoint));
				});

		// 计算并返回积分值
		return volume * sumAdder.sum() / numPoints;
	}

	public static List<Double> CalculateSensitivity2(Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys) {
		// system 里面的dag都是一样的
		List<List<Node>> allpaths = sys.getFirst().getFirst().allpaths;
		List<List<Node>> Candidates = new ArrayList<>();
		List<Node> allnode = sys.getFirst().getFirst().getFlatNodes();
		int nodesize = allnode.size();
		List<Long> IdtoWCET = new ArrayList<>(Collections.nCopies(nodesize, 0L));
		List<Double> ProbabilityList = new ArrayList<>();

		for (Node n : allnode) {
			IdtoWCET.set(n.getId(), n.WCET);
		}

		Long maxlowerbound = (long) 0;
		Long[] upperboundList = new Long[allpaths.size()];

		for (int i = 0; i < allpaths.size(); i++) {
			Long temp = (long) 0;
			for (Node n : allpaths.get(i)) {
				temp += n.WCET;
			}
			upperboundList[i] = temp;
			temp = (long) (temp * 0.3); // 这里设下界就是上界的0.3
			maxlowerbound = temp > maxlowerbound ? temp : maxlowerbound;
		}
		// 假设是正态分布<均值，方差>，方差用上下界决定
		// List<Pair<Integer, Integer>> distributedList = new ArrayList<>();
		for (int i = 0; i < allpaths.size(); i++) {
			if (upperboundList[i] > maxlowerbound) {
				Candidates.add(allpaths.get(i));
			}
		}

		List<Integer> sharenode = new ArrayList<>(Collections.nCopies(nodesize, 0));
		// 在候选路径里面找共享节点！！！！
		for (int i = 0; i < Candidates.size(); i++) {
			for (Node n : Candidates.get(i)) {
				sharenode.set(n.getId(), sharenode.get(n.getId()) + 1); // 大于1的是被共享的节点
			}
		}

		// 开始做json
		List<Object> allData = new ArrayList<>();
		for (int i = 0; i < Candidates.size(); i++) {
			IntegrandVariable = new ArrayList<>();
			Item = new ArrayList<>();
			Item1 = new ArrayList<>();
			Item2 = new ArrayList<>();

			List<Integer> idtoid = new ArrayList<>(Collections.nCopies(nodesize, -1));
			List<Integer> p = new ArrayList<>();
			// Long temp = 0L;
			for (Node n : Candidates.get(i)) {
				p.add(n.getId());
				if (!(sharenode.get(n.getId()) == Candidates.size())) {
					IntegrandVariable.add(IdtoWCET.get(n.getId()).doubleValue());
					idtoid.set(n.getId(), IntegrandVariable.size() - 1);
				}

			}

			for (int j = 0; j < Candidates.size(); j++) {
				List<Double> item = new ArrayList<>();
				List<Integer> item1 = new ArrayList<>();
				List<Integer> item2 = new ArrayList<>();
				if (i == j) {
					continue;
				}
				List<Integer> p2 = new ArrayList<>();
				for (Node n : Candidates.get(j)) {
					if (!p.contains(n.getId())) {
						item.add(IdtoWCET.get(n.getId()).doubleValue());
					}
					p2.add(n.getId());
				}
				for (Integer id : p) {
					if (!p2.contains(id)) {
						item1.add(idtoid.get(id));
					}
				}
				Item.add(item);
				Item1.add(item1);
				Item2.add(item2);
			}
			double[] MINB = new double[IntegrandVariable.size()];
			double[] UPB = new double[IntegrandVariable.size()];

			for (int k = 0; k < IntegrandVariable.size(); k++) {
				MINB[k] = IntegrandVariable.get(k) / 3.0;
				UPB[k] = IntegrandVariable.get(k);
			}

			ProbabilityList.add(parallelMonteCarloIntegration(50000, MINB, UPB));
			Double R = ProbabilityList.getLast();
			System.out.println(R);
			// allData.add(new Object[] { IntegrandVariable, Item, Item1, Item2 });
		}

		List<Double> sensitivitylList = new ArrayList<>();
		for (int i = 0; i < sys.getFirst().getFirst().getFlatNodes().size(); i++) {
			sensitivitylList.add(0.0);
		}

		for (int i = 0; i < Candidates.size(); i++) {
			for (Node n : Candidates.get(i)) {
				sensitivitylList.set(n.getId(), sensitivitylList.get(n.getId()) + ProbabilityList.get(i));
			}
		}

		return sensitivitylList;
	}

	/**
	 * This test case will generate two fixed DAG structure.
	 */
	@SuppressWarnings("unchecked")
	public static OneSystemResults testOneCaseThreeMethod(Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys,
			int tasks, int[] NoInstances, int cores, int taskSeed, int tableSeed, int not) {

		boolean lcif = true;

		double cc_sens = 0;

		// for (int k = 0; k < SystemParameters.cc_weights.length; k++) {
		// cc_sens += SystemParameters.cc_weights[k];//相关系数
		// }
		// //敏感度计算
		// for (DirectedAcyclicGraph d : sys.getFirst()) {
		// for (Node n : d.getFlatNodes()) {
		// n.sensitivity = 0;
		// for (int k = 0; k < n.weights.length; k++) {
		// n.sensitivity += n.weights[k] * SystemParameters.cc_weights[k] / cc_sens;
		// }
		// }
		// }

		// Simualtor sim1 = new Simualtor(SimuType.CLOCK_LEVEL, Hardware.PROC_CACHE,
		// Allocation.WORST_FIT,
		// RecencyType.TIME_DEFAULT, sys.getFirst(), sys.getSecond(), cores, tableSeed,
		// lcif);
		// Pair<List<DirectedAcyclicGraph>, double[]> pair1 = sim1.simulate(print);

		// SimualtorNWC sim2 = new SimualtorNWC(SimuType.CLOCK_LEVEL,
		// Hardware.PROC_CACHE, Allocation.CACHE_AWARE_NEW,
		// RecencyType.TIME_DEFAULT, sys.getFirst(), sys.getSecond(), cores, tableSeed,
		// lcif);
		// Pair<List<DirectedAcyclicGraph>, double[]> pair2 = sim2.simulate(print);

		// SimualtorNWC cacheCASim3 = new SimualtorNWC(SimuType.CLOCK_LEVEL,
		// Hardware.PROC_CACHE, Allocation.CARVB,
		// RecencyType.TIME_DEFAULT, sys.getFirst(), sys.getSecond(), cores, tableSeed,
		// false);
		// Pair<List<DirectedAcyclicGraph>, double[]> pair3 =
		// cacheCASim3.simulate(print);

		// // 正态分布的差也是正态分布
		// List<Double> ProbabilityList = new ArrayList<>();
		// for (int i = 0; i < Candidates.size(); i++) {
		// double Probability = 1;
		// for (int j = 0; j < Candidates.size(); j++) {
		// if (i == j) {
		// continue;
		// }
		// // 去掉相同节点，保持独立性
		// List<Integer> l1 = new ArrayList<>();
		// for (Node n : Candidates.get(i)) {
		// l1.add(n.getId());
		// }

		// List<Integer> l2 = new ArrayList<>();
		// for (Node n : Candidates.get(j)) {
		// l2.add(n.getId());
		// }

		// // 收集需要移除的节点 ID
		// List<Integer> toRemoveFromL1l2 = new ArrayList<>();

		// for (Node n : Candidates.get(i)) {
		// if (l2.contains(n.getId())) {
		// toRemoveFromL1l2.add(n.getId());
		// }
		// }

		// // 从 l1 和 l2 中批量移除相同的节点 ID
		// l1.removeAll(toRemoveFromL1l2);
		// l2.removeAll(toRemoveFromL1l2);
		// double coefficient = 3; // 设定几sigam拒绝

		// double mu1 = 0;
		// double sigma12 = 0;
		// for (int id : l1) {
		// Long W = WCETlList.get(id);
		// double t = W * (0.3 + 1) / 2;
		// mu1 += t;
		// sigma12 += (1 / coefficient) * (1 / coefficient) * (W - t) * (W - t);
		// }

		// double mu2 = 0;
		// double sigma22 = 0;
		// for (int id : l2) {
		// Long W = WCETlList.get(id);
		// double t = W * (0.3 + 1) / 2;
		// mu2 += t;
		// sigma22 += (1 / coefficient) * (1 / coefficient) * (W - t) * (W - t);
		// }

		// double muxy = mu2 - mu1;
		// double sigmaxy = Math.sqrt(sigma12 + sigma22);
		// // 构造一个正太分布
		// NormalDistribution sumDist = new NormalDistribution(muxy, sigmaxy);
		// double cdf = sumDist.cumulativeProbability(0);
		// Probability *= cdf * 2;// 避免概率太小梯度消失（感觉和梯度消失有点像\(^o^)/~
		// }
		// // Probability+=1;
		// ProbabilityList.add(Probability);
		// }

		List<Double> sensitivitylList = CalculateSensitivity2(sys);

		// 只给前10%的节点（MSF)，后百分90%用MCF,sensitivityL设为1
		List<Double> temp = sensitivitylList;
		temp.sort(Comparator.reverseOrder());
		Double tn = temp.get((int) 30 * temp.size() / 100);
		for (DirectedAcyclicGraph d : sys.getFirst()) {
			for (Node n : d.getFlatNodes()) {
				n.sensitivity = sensitivitylList.get(n.getId());
				if (n.sensitivity >= tn) {
					n.sensitivityL = 1;
				}
			}
		}
		// OnlineCARVB.etHistOneNode=new ArrayList<>();

		SimualtorNWC cacheCASim1 = new SimualtorNWC(SimuType.CLOCK_LEVEL, Hardware.PROC_CACHE,
				Allocation.CACHE_AWARE_NEW,
				RecencyType.TIME_DEFAULT, sys.getFirst(), sys.getSecond(), cores, tableSeed, false);
		Pair<List<DirectedAcyclicGraph>, double[]> pair1 = cacheCASim1.simulate(print);
		// OnlineCARVB.etHistOneNode = new ArrayList<>();

		SimualtorNWC cacheCASim2 = new SimualtorNWC(SimuType.CLOCK_LEVEL, Hardware.PROC_CACHE, Allocation.CARVB,
				RecencyType.TIME_DEFAULT, sys.getFirst(), sys.getSecond(), cores, tableSeed, false);
		Pair<List<DirectedAcyclicGraph>, double[]> pair2 = cacheCASim2.simulate(print);
		// OnlineCARVB.etHistOneNode = new ArrayList<>();

		// SystemParameters.m = 4;
		// SimualtorNWC cacheCASim3 = new SimualtorNWC(SimuType.CLOCK_LEVEL,
		// Hardware.PROC_CACHE, Allocation.CARVB_SEEN,
		// RecencyType.TIME_DEFAULT, sys.getFirst(), sys.getSecond(), cores, tableSeed,
		// false);
		// Pair<List<DirectedAcyclicGraph>, double[]> pair3 =
		// cacheCASim3.simulate(print);
		// OnlineCARVB_SEEN.etHistOneNode = new ArrayList<>();

		SimualtorNWC cacheCASim4 = new SimualtorNWC(SimuType.CLOCK_LEVEL, Hardware.PROC_CACHE, Allocation.CARVB_MSF,
				RecencyType.TIME_DEFAULT, sys.getFirst(), sys.getSecond(), cores, tableSeed, false);
		Pair<List<DirectedAcyclicGraph>, double[]> pair4 = cacheCASim4.simulate(print);
		// OnlineCARVB.etHistOneNode = new ArrayList<>();

		List<DirectedAcyclicGraph> m1 = pair1.getFirst();
		List<DirectedAcyclicGraph> m2 = pair2.getFirst();
		// List<DirectedAcyclicGraph> m3 = pair3.getFirst();
		List<DirectedAcyclicGraph> m4 = pair4.getFirst();

		List<List<DirectedAcyclicGraph>> allMethods = new ArrayList<>();

		List<DirectedAcyclicGraph> method1 = new ArrayList<>();
		List<DirectedAcyclicGraph> method2 = new ArrayList<>();
		List<DirectedAcyclicGraph> method3 = new ArrayList<>();
		List<DirectedAcyclicGraph> method4 = new ArrayList<>();

		List<DirectedAcyclicGraph> dags = sys.getFirst();

		/*
		 * get a number of instances from each DAG based on long[] NoInstances.
		 */
		int count = 0;
		int currentID = -1;
		for (int i = 0; i < dags.size(); i++) {
			if (currentID != dags.get(i).id) {

				currentID = dags.get(i).id;
				count = 0;
			}

			if (count < NoInstances[dags.get(i).id]) {
				method1.add(m1.get(i));
				method2.add(m2.get(i));
				// method3.add(m3.get(i));
				method4.add(m4.get(i));

				count++;
			}
		}

		allMethods.add(method1);
		allMethods.add(method2);
		// allMethods.add(method3);
		allMethods.add(method4);

		List<double[]> cachePerformance = new ArrayList<>();
		cachePerformance.add(pair1.getSecond());
		cachePerformance.add(pair2.getSecond());
		// cachePerformance.add(pair3.getSecond());
		cachePerformance.add(pair4.getSecond());

		OneSystemResults result = new OneSystemResults(allMethods, cachePerformance);

		return result;
	}
}
