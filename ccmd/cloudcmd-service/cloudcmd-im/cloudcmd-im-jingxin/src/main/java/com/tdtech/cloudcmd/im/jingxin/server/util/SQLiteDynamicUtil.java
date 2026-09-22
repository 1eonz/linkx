package com.tdtech.cloudcmd.im.jingxin.server.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * @author lsc
 * @date 2025/9/25
 **/
@Component
@Slf4j
public class SQLiteDynamicUtil {
    private static final String DRIVER_CLASS = "org.sqlite.JDBC";
    private static final String DEFAULT_DB_PATH = "D:\\sqlite\\cloudcmd.db";
    private static final String DEFAULT_DB_NAME = "cloudcmd.db";
    private static final String DROP_TABLE_SQL_TB_CHAT_GROUP = "drop table if exists tb_chat_group_?";
    private static final String DROP_TABLE_SQL_TB_CHAT_MEMBER = "drop table if exists tb_chat_member_?";
    private static final String CREATE_TABLE_SQL_TB_CHAT_MEMBER = "CREATE TABLE IF NOT EXISTS tb_chat_member_? (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "code TEXT NOT NULL," +  // 成员唯一编码（替代user_id的唯一标识作用）
            "name TEXT NOT NULL," +
            "alias TEXT," +
            "tumb_avatar TEXT," +
            "gender INTEGER," +
            "gender_name TEXT," +
            "mobile TEXT," +
            "email TEXT," +
            "isdn TEXT," +
            "direct_leader_id INTEGER," +
            "status INTEGER," +
            "status_name TEXT," +
            "user_alias TEXT," +
            "idcard TEXT," +
            "role INTEGER DEFAULT 0," +
            "mute_type INTEGER DEFAULT 0," +
            "join_type INTEGER," +
            "invite_id INTEGER," +  // 关联同表的id（邀请人）
            "invite_time INTEGER" +
            ")";
    private static final String CREATE_TABLE_SQL_TB_CHAT_GROUP = "CREATE TABLE IF NOT EXISTS tb_chat_group_? (" +
            "category INTEGER NOT NULL," +
            "msg_type INTEGER NOT NULL," +
            "`from` INTEGER NOT NULL," +  // 反引号处理SQL关键字
            "from_real_user_id TEXT," +
            "`to` INTEGER NOT NULL,"  +    // 反引号处理SQL关键字
            "from_isdn TEXT," +
            "to_isdn TEXT," +
            "forward_msg INTEGER," +       // SQLite无BOOLEAN类型，用0/1存储
            "from_type INTEGER," +
            "to_type INTEGER," +
            "one_by_one_msg INTEGER," +
            "client_msg_id TEXT," +  // 客户端消息ID唯一
            "plaintext INTEGER," +
            "msg_id TEXT," +
            "time INTEGER NOT NULL," +
            "seq INTEGER," +
            "session_seq_id INTEGER," +
            "read INTEGER ," +    // 0=未读，1=已读
            "withdraw INTEGER ," + // 0=未撤回，1=已撤回
            "msg TEXT NOT NULL" +           // 消息内容（存储JSON字符串）
            ")";

    @Autowired
    private EncryptImUtil encryptImUtil;

    public Connection getConnection(String dbPath) {
        Connection connection = null;
        try {
            Class.forName(DRIVER_CLASS);

            // 连接数据库（如果不存在则自动创建）
            String url = "jdbc:sqlite:" + dbPath;
            connection = DriverManager.getConnection(url);

        } catch (Exception e) {
            log.error("数据库错误: " + e.getMessage());
        }
        return connection;
    }

    /**
     * 在指定数据库中创建表
     * @param dbPath 数据库文件路径
     * @param tableName 表名
     * @return 是否创建成功
     */
    public boolean createTable(String dbPath, String tableName, Long groupId) {
        Connection connection = null;
        Statement statement = null;
        try {

            connection = getConnection(dbPath);
            statement = connection.createStatement();
            if(tableName.equals("tb_chat_member_" + groupId)){
                String createTableSQL = CREATE_TABLE_SQL_TB_CHAT_MEMBER.replace("?", groupId.toString());
                statement.execute(createTableSQL);
            }if(tableName.equals("tb_chat_group_" + groupId)){
                String createTableSQL = CREATE_TABLE_SQL_TB_CHAT_GROUP.replace("?", groupId.toString());
                statement.execute(createTableSQL);
            }

            // 执行创建表SQL
            log.info("表 " + tableName + " 创建成功");
            return true;
        } catch (Exception e) {
            log.error("创建表错误: " + e.getMessage());
        } finally {
            try {
                if (statement != null){
                    statement.close();
                }
                if (connection != null){
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("关闭资源错误: " + e.getMessage());
            }
        }
        return false;
    }

    public boolean dropTable(String dbPath, String tableName, Long groupId) {
        Connection connection = null;
        Statement statement = null;
        try {

            connection = getConnection(dbPath);
            statement = connection.createStatement();
            if(tableName.equals("tb_chat_member_" + groupId)){
                String sql = DROP_TABLE_SQL_TB_CHAT_MEMBER.replace("?", groupId.toString());
                statement.execute(sql);
            }if(tableName.equals("tb_chat_group_" + groupId)){
                String sql = DROP_TABLE_SQL_TB_CHAT_GROUP.replace("?", groupId.toString());
                statement.execute(sql);
            }

            // 执行SQL
            log.info("表 " + tableName + " 删除成功");
            return true;
        } catch (Exception e) {
            log.error("删除表错误: " + e.getMessage());
        } finally {
            try {
                if (statement != null){
                    statement.close();
                }
                if (connection != null){
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("删除表后关闭资源错误: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * 分页查询数据
     * @param dbPath 数据库路径
     * @param tableName 表名
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数
     * @return 分页查询结果，包含当前页数据和分页信息
     */
    public Map<String, Object> queryByPage(String dbPath, String tableName,
                                           int pageNum, int pageSize) {
        return queryByPageWithCondition(dbPath, tableName, null, null, pageNum, pageSize);
    }
    /**
     * 带条件的分页查询
     * @param groupPath 数据库路径目录
     * @param dbFileName 数据库文件名称
     * @param tableName 表名
     * @param condition 查询条件（如 "age > 18"，不带WHERE关键字）
     * @param orderBy 排序条件（如 "id DESC"，不带ORDER BY关键字）
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数
     * @return 分页查询结果，包含当前页数据和分页信息
     */
    public Map<String, Object> queryByPageWithConditionEncrypt(String groupPath, String dbFileName,
                                                        String tableName, String condition, String orderBy,
                                                        int pageNum, int pageSize, String groupId) {

        String tempFilePath = groupPath + File.separator + dbFileName;
        if (!encryptImUtil.encryptEnabled()) {
            return queryByPageWithCondition(tempFilePath, tableName, condition, orderBy, pageNum, pageSize);
        }
        // 获取可读取的临时文件, 执行查询
        boolean encryptFileExists = encryptImUtil.isEncryptFileExists(groupPath, dbFileName);
        if (!encryptFileExists) {
            return Collections.emptyMap();
        }
        String fileToRead = encryptImUtil.getFileToRead(groupPath, dbFileName, groupId);
        return queryByPageWithCondition(fileToRead, tableName, condition, orderBy, pageNum, pageSize);
    }


    /**
     * 带条件的分页查询
     * @param dbPath 数据库路径
     * @param tableName 表名
     * @param condition 查询条件（如 "age > 18"，不带WHERE关键字）
     * @param orderBy 排序条件（如 "id DESC"，不带ORDER BY关键字）
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数
     * @return 分页查询结果，包含当前页数据和分页信息
     */
    private Map<String, Object> queryByPageWithCondition(String dbPath, String tableName,
                                                        String condition, String orderBy,
                                                        int pageNum, int pageSize) {
        // 结果封装：包含数据列表和分页信息
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> dataList = new ArrayList<>();

        // 校验分页参数
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }

        // 计算偏移量
        int offset = (pageNum - 1) * pageSize;

        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection(dbPath);

            // 构建SQL语句
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);

            // 添加查询条件
            if (condition != null && !condition.trim().isEmpty()) {
                sql.append(" WHERE ").append(condition);
            }

            // 添加排序
            if (orderBy != null && !orderBy.trim().isEmpty()) {
                sql.append(" ORDER BY ").append(orderBy);
            }

            // 添加分页（LIMIT 条数 OFFSET 偏移量）
            sql.append(" LIMIT ? OFFSET ?");

            log.debug("执行分页SQL: " + sql);

            pstmt = connection.prepareStatement(sql.toString());
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, offset);

            resultSet = pstmt.executeQuery();

            // 处理查询结果
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                dataList.add(row);
            }

            // 查询总记录数
            int total = getTotalCount(connection, tableName, condition);
            // 计算总页数
            int totalPages = (total + pageSize - 1) / pageSize;

            // 封装分页信息
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("total", total);
            result.put("totalPages", totalPages);

            result.put("data", dataList);

            log.debug("分页查询成功：第" + pageNum + "页，共" + totalPages + "页");

        } catch (Exception e) {
            System.err.println("分页查询错误: " + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (pstmt != null){
                    pstmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("关闭资源错误: " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * 查询总记录数
     * @param connection 数据库连接
     * @param tableName 表名
     * @param condition 查询条件（如 "age > 18"，不带WHERE关键字）
     * @return 符合条件的总记录数
     */
    private int getTotalCount(Connection connection, String tableName, String condition)
            throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM ").append(tableName);

            if (condition != null && !condition.trim().isEmpty()) {
                sql.append(" WHERE ").append(condition);
            }

            pstmt = connection.prepareStatement(sql.toString());
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
        }

        return 0;
    }

    /**
     * 插入单条数据
     * @param dbPath 数据库路径
     * @param tableName 表名
     * @param data 要插入的数据，key为列名，value为对应值
     * @return 插入是否成功
     */
    public boolean insertData(String dbPath, String tableName, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            System.err.println("插入数据不能为空");
            return false;
        }

        Connection connection = null;
        PreparedStatement pstmt = null;

        try {
            connection = getConnection(dbPath);

            // 构建插入SQL
            StringBuilder columns = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();

            Set<String> columnNames = data.keySet();
            int index = 0;
            for (String column : columnNames) {
                if (index > 0) {
                    columns.append(", ");
                    placeholders.append(", ");
                }
                columns.append(column);
                placeholders.append("?");
                index++;
            }

            String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
            log.debug("执行插入SQL: " + sql);

            pstmt = connection.prepareStatement(sql);

            // 设置参数
            index = 1;
            for (Object value : data.values()) {
                pstmt.setObject(index++, value);
            }

            // 执行插入
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            log.error("插入数据错误: " + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("关闭资源错误: " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * 按条件删除数据
     * @param dbPath 数据库路径
     * @param tableName 表名
     * @param condition 删除条件（如 "id = 1" 或 "age < 18"）
     * @param params 条件参数（用于参数化查询，防止SQL注入）
     * @return 删除的记录数
     */
    public int deleteData(String dbPath, String tableName, String condition, Object[] params) {
        Connection connection = null;
        PreparedStatement pstmt = null;

        try {
            connection = getConnection(dbPath);

            // 构建删除SQL
            String sql = "DELETE FROM " + tableName ;
            if (condition != null && !condition.trim().isEmpty()) {
                sql = sql + " WHERE " + condition;
            }
            log.debug("执行删除SQL: " + sql);

            pstmt = connection.prepareStatement(sql);

            // 设置参数
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
            }
            // 执行删除
            int rowsAffected = pstmt.executeUpdate();
            log.debug("成功删除 " + rowsAffected + " 条记录");
            return rowsAffected;

        } catch (Exception e) {
            log.debug("删除数据错误: " + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("关闭资源错误: " + e.getMessage());
            }
        }

        return 0;
    }

    /**
     * 按ID删除数据（简化方法）
     * @param dbPath 数据库路径
     * @param tableName 表名
     * @param id 要删除的记录ID
     * @return 是否删除成功
     */
    public boolean deleteById(String dbPath, String tableName, int id) {
        return deleteData(dbPath, tableName, "id = ?", new Object[]{id}) > 0;
    }

    public Map<String, Object> getGroupMessageByIdEncrypt(String groupPath, String dbFileName,
                                                          String tableName, String groupId,
                                                          String condition) {
        String tempFilePath = groupPath + File.separator + dbFileName;
        if (!encryptImUtil.encryptEnabled()) {
            return getGroupMessageById(tempFilePath, tableName, condition);
        }
        // 获取可读取的临时文件, 执行查询
        String fileToRead = encryptImUtil.getFileToRead(groupPath, dbFileName, groupId);
        return getGroupMessageById(fileToRead, tableName, condition);
    }


    private Map<String, Object> getGroupMessageById(String dbPath, String tableName,String condition) {
        // 结果封装：包含数据列表和分页信息
        List<Map<String, Object>> dataList = new ArrayList<>();


        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection(dbPath);

            // 构建SQL语句
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);

            // 添加查询条件
            if (condition != null && !condition.trim().isEmpty()) {
                sql.append(" WHERE ").append(condition);
            }

            log.debug("执行SQL: " + sql);

            pstmt = connection.prepareStatement(sql.toString());

            resultSet = pstmt.executeQuery();

            // 处理查询结果
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                dataList.add(row);
            }
        } catch (Exception e) {
            log.error("查询数据错误: " + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (pstmt != null){
                    pstmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("关闭资源错误: " + e.getMessage());
            }
        }
        return dataList.get(0);
    }

    /**
     * 查询tb_chat_group表中最大的session_seq_id
     * @param basePath 文件基础路径
     * @param groupId 群组ID
     * @return 最大的session_seq_id，如果表为空或不存在则返回0
     */
    public Long getMaxSessionSeqIdEncrypt(String basePath, String dbFileName, Long groupId) {
        String groupPath = encryptImUtil.getGroupPath(basePath, String.valueOf(groupId));
        String tempFilePath = groupPath + File.separator + dbFileName;
        if (!encryptImUtil.encryptEnabled()) {
            return getMaxSessionSeqId(tempFilePath, groupId);
        }
        // 获取可读取的临时文件, 执行查询
        String fileToRead = encryptImUtil.getFileToRead(groupPath, dbFileName, groupId.toString());
        Long maxSessionSeqId = getMaxSessionSeqId(fileToRead, groupId);
        // 删除读临时文件
        encryptImUtil.clearFile(fileToRead);
        return maxSessionSeqId;
    }

    /**
     * 查询tb_chat_group表中最大的session_seq_id
     * @param dbPath 数据库路径
     * @param groupId 群组ID
     * @return 最大的session_seq_id，如果表为空或不存在则返回0
     */
    public Long getMaxSessionSeqId(String dbPath, Long groupId) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection(dbPath);

            // 构建表名
            String tableName = "tb_chat_group_" + groupId;

            // 查询最大的session_seq_id
            String sql = "SELECT MAX(session_seq_id) AS max_seq_id FROM " + tableName;
            log.debug("执行SQL: " + sql);

            pstmt = connection.prepareStatement(sql);
            resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                Long maxSeqId = resultSet.getLong("max_seq_id");
                // 如果结果为NULL（表为空），返回0
                if (resultSet.wasNull()) {
                    return 0L;
                }
                return maxSeqId;
            }

        } catch (Exception e) {
            log.error("查询最大session_seq_id错误: " + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("关闭资源错误: " + e.getMessage());
            }
        }

        return 0L;
    }
}
