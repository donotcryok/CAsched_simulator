import json
import numpy as np
from scipy import integrate
from scipy.stats import norm
import math
from multiprocessing import Pool, cpu_count

import vegas

IntegrandVariable=[] #被积分函数
Item=[]  #要求累计分布的变量，二维数组
Item1=[]  #F()里面，正的变量的index
Item2=[]  #F()里面，负的变量的index(共享变量)
Path=[]
from joblib import Parallel, delayed

import vegas
import numpy as np

def vegas_integrate(f, bounds, nitn=10, neval=1e4, verbose=False):
    """
    使用Vegas库进行高维积分（支持自动自适应采样）

    参数：
    - f: 被积函数，输入为numpy数组（各维度的值），输出为标量
    - bounds: 积分区域边界，格式为[[a1, b1], [a2, b2], ..., [an, bn]]
    - nitn: 自适应迭代次数（默认10）
    - neval: 每次迭代的采样数（默认1e4）
    - verbose: 是否打印详细结果（默认True）

    返回：
    - (积分均值, 标准差估计)
    """
    # 检查输入合法性
    assert len(bounds) > 0, "积分维度不能为0"
    for i, b in enumerate(bounds):
        if len(b) != 2:
            raise ValueError(f"第{i}维的边界 {b} 格式错误，应为 [lower, upper]")
        if b[0] >= b[1]:
            raise ValueError(f"第{i}维的边界 {b} 需满足 lower < upper")

    dim = len(bounds)
    
    # 定义Vegas适配函数（将字典输入转为numpy数组）
    def _wrapped_f(x):
        return f(np.array([x[i] for i in range(dim)]))
    
    # 初始化积分器
    integrator = vegas.Integrator(bounds)
    
    # 执行积分
    result = integrator(_wrapped_f, nitn=nitn, neval=int(neval))
    
    # 输出结果
    if verbose:
        print(result.summary())
        print(f"积分结果: {result.mean:.6f} ± {result.sdev:.6f}")
    
    return result.mean, result.sdev
import vegas
import numpy as np
from multiprocessing import Pool, cpu_count

def vegas_integrate_parallel(f, bounds, nitn=10, neval=1e4, verbose=True, n_jobs=None):
    """
    并行化Vegas高维积分（利用多进程加速函数评估）

    参数：
    - f: 被积函数，输入为numpy数组（各维度的值），输出为标量
    - bounds: 积分区域边界，格式为[[a1, b1], [a2, b2], ..., [an, bn]]
    - nitn: 自适应迭代次数（默认10）
    - neval: 每次迭代的采样数（默认1e4）
    - verbose: 是否打印详细结果（默认True）
    - n_jobs: 并行进程数（默认使用全部CPU核心）

    返回：
    - (积分均值, 标准差估计)
    """
    # 输入检查（同上，此处省略重复代码）
    assert len(bounds) > 0, "积分维度不能为0"
    for i, b in enumerate(bounds):
        if len(b) != 2:
            raise ValueError(f"第{i}维的边界 {b} 格式错误，应为 [lower, upper]")
        if b[0] >= b[1]:
            raise ValueError(f"第{i}维的边界 {b} 需满足 lower < upper")
    
    dim = len(bounds)
    n_jobs = cpu_count() if n_jobs is None else n_jobs
    
    # 定义并行评估函数
    def _parallel_eval(points):
        """
        并行评估函数：将多个积分点分块后通过进程池计算
        points: 待计算的点列表（每个点为字典形式）
        """
        # 将Vegas的点字典转为numpy数组
        x_list = [np.array([p[i] for i in range(dim)]) for p in points]
        
        # 分块处理以减少通信开销
        chunk_size = max(1, len(x_list) // (n_jobs * 2))
        with Pool(n_jobs) as pool:
            results = pool.map(f, x_list, chunksize=chunk_size)
        return np.array(results)
    
    # 初始化积分器并设置并行评估
    integrator = vegas.Integrator(bounds)
    integrator.map = _parallel_eval  # 关键：替换默认的串行评估函数
    
    # 执行积分
    result = integrator(f, nitn=nitn, neval=int(neval))
    
    # 输出结果（同上，此处省略）
    if verbose:
        print(result.summary())
        print(f"积分结果: {result.mean:.6f} ± {result.sdev:.6f}")
    
    return result.mean, result.sdev

# 定义蒙特卡洛积分的并行化版本
def parallel_monte_carlo_integration(func, bounds, n_samples=100000, n_jobs=-1):
    dim = len(bounds)
    samples = np.random.uniform(low=[b[0] for b in bounds], high=[b[1] for b in bounds], size=(n_samples, dim))
    
    # 使用并行计算来处理采样计算
    values = Parallel(n_jobs=n_jobs)(delayed(func)(*sample) for sample in samples)
    volume = np.prod([b[1] - b[0] for b in bounds])
    return np.mean(values) * volume




def monte_carlo_integration(func, bounds, n_samples=50000):
    dim = len(bounds)
    samples = np.random.uniform(low=[b[0] for b in bounds], high=[b[1] for b in bounds], size=(n_samples, dim))
    values = np.array([func(*sample) for sample in samples])
    volume = np.prod([b[1] - b[0] for b in bounds])
    return np.mean(values) * volume

# 定义一个适应多维积分的函数
def integrand(*args):
    Y=1
    for x,x1,x2 in zip(Item,Item1,Item2):

        sum_result = sum(args[i] for i in x1)-sum(args[i] for i in x2)
        if len(x)>0:
            #计算正太分布的，平均值和标准差
            mean=sum(x)*(1+1/3)/2
            std2=0
            for k in x:
                std2+=(k*(1-1/3)/(2*3))*(k*(1-1/3)/(2*3))
            Y*=norm.cdf(sum_result, loc=mean, scale=math.sqrt(std2))
        else:
            Y*=sum_result>0
            
    for x,WCET in zip(args,IntegrandVariable):
        Y*= norm.pdf(x,WCET*(1+1/3)/2,WCET*(1-1/3)/(2*3))

    return Y
    # return np.exp(-sum(x**2 for x in args))  # 计算每个变量平方和的指数



# # 计算从负无穷到 1 的积分
# result, error = integrate.quad(normal_pdf, -np.inf, 1, args=(0, 1))


if __name__ == "__main__":
    # 读取 JSON 文件
    with open('data.json', 'r') as f:
        data = json.load(f)
    Probability=[]
    # 输出数据
    for idx, entry in enumerate(data):
        IntegrandVariable=entry[0]
        Item=entry[1]  
        Item1=entry[2]  
        Item2=entry[3] 
        bounds = [[x / 3,x] for x in IntegrandVariable]
        # 计算积分
        result= monte_carlo_integration(integrand, bounds)
        # result=parallel_monte_carlo_integration(integrand, bounds)
        # 自适应训练阶段，提高采样效率
        # result,error = vegas_integrate(integrand, bounds)
        # result,error =vegas_integrate_parallel(integrand, bounds)
        # result,error = integrate.nquad(integrand, bounds)
        

        Probability.append(result)
        print("积分结果:", result)
        # print("误差估计:", error)
        
with open('output.json', 'w') as f:
    json.dump(Probability, f)

    

    