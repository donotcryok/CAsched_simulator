% 示例数据
dataDis1 = rand(100, 5); % 生成一个100x5的随机二维数组作为示例数据
dataDis1
% 获取数组的大小
[rows, cols] = size(dataDis1);

% 对每一列进行处理
for col = 1:cols
    % 获取当前列的数据
    column_data = dataDis1(:, col);
    
    % 获取当前列数据的排序索引
    [~, sorted_indices] = sort(column_data, 'descend');
    
    % 计算本列最大的前10%数据的索引范围
    top_10_percent_index = sorted_indices(1:ceil(0.1 * rows));
    
    % 将不在前10%索引范围内的数据赋值为0，否则赋值为1
    dataDis1(:, col) = ismember(1:rows, top_10_percent_index)';
end

dataDis1