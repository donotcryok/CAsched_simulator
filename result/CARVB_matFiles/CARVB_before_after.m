clc
close all

core = 4;
colors=["b"; "r"];
types = ["$C_j$","$\hat{L}(v_j)$","$D^{in}_j$","$D^{out}_j$","$D_j$","$||G(v_j)||$"];
types_names = ["nodeET","pathET","in-degree","out-degree","in_out_degree","pathNum"];
judgements = 0.9;
dummy_judgement = "0.1";

threads = 0:0;
effects = ["0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0"];

allData = zeros(0);
for effect = effects
    for thread = threads
        data = readmatrix(strcat('../gyy/random_',num2str(core),'_',dummy_judgement,'_',effect,'_',num2str(thread),'.txt'));
        allData = [allData; data()];
    end
end

[datarow, datacol] = size(allData);
dataDis1 = allData(:,[1:2:datacol-2, datacol]);

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


[r,p] = corrcoef(dataDis1);
r(:,7)'


% allData = zeros(0);
% for effect = effects
%     for thread = threads
%         data = readmatrix(strcat('../gyy/random_',num2str(core),'_',dummy_judgement,'_',effect,'_',num2str(thread),'.txt'));
%         allData = [allData; data()];
%     end
%     [datarow, datacol] = size(allData);
%     dataDis1 = allData(:,[1:2:datacol-2, datacol]);
%     
%     % 获取数组的大小
%     [rows, cols] = size(dataDis1);
%     
%     % 对每一列进行处理
%     for col = 1:cols
%         % 获取当前列的数据
%         column_data = dataDis1(:, col);
%         
%         % 获取当前列数据的排序索引
%         [~, sorted_indices] = sort(column_data, 'descend');
%         
%         % 计算本列最大的前10%数据的索引范围
%         top_10_percent_index = sorted_indices(1:ceil(0.1 * rows));
%         
%         % 将不在前10%索引范围内的数据赋值为0，否则赋值为1
%         dataDis1(:, col) = ismember(1:rows, top_10_percent_index)';
%     end
% 
% 
%     [r,p] = corrcoef(dataDis1);
%     r(:,7)'
% end
% [datarow, datacol] = size(allData);
% dataDis1 = allData(:,[1:2:datacol-2, datacol]);
% [r,p] = corrcoef(dataDis1);
% r(:,7)'




% allData = zeros(0);
% for effect = effects
%     for thread = threads
%         data = readmatrix(strcat('../faults_new/before_after_ajlr_ajlr',num2str(core),'_',dummy_judgement,'_',effect,'_',num2str(thread),'.txt'));
%         allData = [allData; data()];
%     end
% end
% [datarow, datacol] = size(allData);
% dataDis1 = allData(:,[1:2:datacol-2, datacol]);
% [r,p] = corrcoef(dataDis1);
% r(:,7)'

% allData = zeros(0);
% for effect = effects
%     for thread = threads
%         data = readmatrix(strcat('../faults_new/before_after_carvb_carvb_',num2str(core),'_',dummy_judgement,'_',effect,'_',num2str(thread),'.txt'));
%         allData = [allData; data()];
%     end
% end
% [datarow, datacol] = size(allData);
% dataDis1 = allData(:,[1:2:datacol-2, datacol]);
% [r,p] = corrcoef(dataDis1);
% r(:,7)'
% 
% allData = zeros(0);
% for effect = effects
%     for thread = threads
%         data = readmatrix(strcat('../faults_new/before_after_carvb_ajlr_',num2str(core),'_',dummy_judgement,'_',effect,'_',num2str(thread),'.txt'));
%         allData = [allData; data()];
%     end
% end
% [datarow, datacol] = size(allData);
% dataDis1 = allData(:,[1:2:datacol-2, datacol]);
% [r,p] = corrcoef(dataDis1);
% r(:,7)'
% 
% allData = zeros(0);
% for effect = effects
%     for thread = threads
%         data = readmatrix(strcat('../faults_new/before_after_ajlr_carvb_',num2str(core),'_',dummy_judgement,'_',effect,'_',num2str(thread),'.txt'));
%         allData = [allData; data()];
%     end
% end
% [datarow, datacol] = size(allData);
% dataDis1 = allData(:,[1:2:datacol-2, datacol]);
% [r,p] = corrcoef(dataDis1);
% r(:,7)'