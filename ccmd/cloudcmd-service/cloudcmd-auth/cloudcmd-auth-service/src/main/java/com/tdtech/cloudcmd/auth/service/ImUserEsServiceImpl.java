package com.tdtech.cloudcmd.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.auth.dto.ImUserDeptQO;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.dto.ImUserQO;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.ImUserES;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.im.jingxin.api.DepartmentRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * im用户es查询服务实现类
 */
@Service
@Slf4j
public class ImUserEsServiceImpl implements ImUserEsService {

    @Autowired
    private ElasticsearchRestTemplate template;

    @Autowired
    private DepartmentRpcApi departmentRpcApi;

    @Override
    public IPage<ImUserDO> queryPaged(IPage<ImUserDO> page, ImUserQO qo) {
        // 构建ES查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();


        // departmentId in查询（对应MySQL IN）
        if (qo.getPrivString() != null && !qo.getPrivString().isBlank()) {
            List<String> priv = Arrays.asList(qo.getPrivString().split(","));
            boolQuery.must(QueryBuilders.termsQuery("departmentId", priv));
        }

        // name模糊查询（对应MySQL LIKE '%name%'）
        if (qo.getName() != null && !qo.getName().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("name", "*" + qo.getName() + "*"));
        }

        // code模糊查询（对应MySQL LIKE '%code%'）
        if (qo.getCode() != null && !qo.getCode().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("code", "*" + qo.getCode() + "*"));
        }

        // idCard模糊查询（对应MySQL LIKE '%idCard%'）
        if (qo.getIdCard() != null && !qo.getIdCard().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("idCard", "*" + qo.getIdCard() + "*"));
        }

        // mobile模糊查询（对应MySQL LIKE '%mobile%'）
        if (qo.getMobile() != null && !qo.getMobile().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("mobile", "*" + qo.getMobile() + "*"));
        }

        // email模糊查询（对应MySQL LIKE '%email%'）
        if (qo.getEmail() != null && !qo.getEmail().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("email", "*" + qo.getEmail() + "*"));
        }

        // departmentCode模糊查询（对应MySQL LIKE '%departmentCode%'）
        if (qo.getDepartmentCode() != null && !qo.getDepartmentCode().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("departmentCode", "*" + qo.getDepartmentCode() + "*"));
        }

        // departmentName模糊查询（对应MySQL LIKE '%departmentName%'）
        if (qo.getDepartmentName() != null && !qo.getDepartmentName().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("departmentName", "*" + qo.getDepartmentName() + "*"));
        }

        // type != 0（对应MySQL !=）
        boolQuery.mustNot(QueryBuilders.termQuery("type", 0));

        // 构建查询
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of((int) page.getCurrent() - 1, (int) page.getSize()))
                .withSort(SortBuilders.fieldSort("gmtUpdated").order(SortOrder.DESC));

        return getImUserDOPage(page, queryBuilder);
    }

    @Override
    public IPage<ImUserDO> queryPagedWithChildren(IPage<ImUserDO> page, ImUserDeptQO qo) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        boolean hasDeptCode = qo.getDepartmentCode() != null && !qo.getDepartmentCode().isBlank();
        boolean expandChildren = hasDeptCode && qo.getIsChildren() != null && qo.getIsChildren() == 1;

        if (expandChildren) {
            List<Long> effectiveDeptIds = expandDepartmentIds(qo.getDepartmentCode());
            if (effectiveDeptIds != null && !effectiveDeptIds.isEmpty()) {
                boolQuery.must(QueryBuilders.termsQuery("departmentId", effectiveDeptIds));
            }
        } else {
            if (qo.getPrivString() != null && !qo.getPrivString().isBlank()) {
                List<String> priv = Arrays.asList(qo.getPrivString().split(","));
                boolQuery.must(QueryBuilders.termsQuery("departmentId", priv));
            }
            if (hasDeptCode) {
                boolQuery.must(QueryBuilders.wildcardQuery("departmentCode", "*" + qo.getDepartmentCode() + "*"));
            }
        }

        if (qo.getName() != null && !qo.getName().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("name", "*" + qo.getName() + "*"));
        }

        if (qo.getCode() != null && !qo.getCode().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("code", "*" + qo.getCode() + "*"));
        }

        if (qo.getIdCard() != null && !qo.getIdCard().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("idCard", "*" + qo.getIdCard() + "*"));
        }

        if (qo.getMobile() != null && !qo.getMobile().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("mobile", "*" + qo.getMobile() + "*"));
        }

        if (qo.getEmail() != null && !qo.getEmail().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("email", "*" + qo.getEmail() + "*"));
        }

        if (!expandChildren && qo.getDepartmentName() != null && !qo.getDepartmentName().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("departmentName", "*" + qo.getDepartmentName() + "*"));
        }

        boolQuery.mustNot(QueryBuilders.termQuery("type", 0));

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of((int) page.getCurrent() - 1, (int) page.getSize()))
                .withSort(SortBuilders.fieldSort("gmtUpdated").order(SortOrder.DESC));

        return getImUserDOPage(page, queryBuilder);
    }

    private List<Long> expandDepartmentIds(String departmentCode) {
        List<OrganizationVO> orgList = departmentRpcApi.queryDepartmentForList(departmentCode);
        if (orgList != null && !orgList.isEmpty()) {
            return orgList.stream()
                .map(OrganizationVO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public IPage<ImUserDO> queryAdmin(IPage<ImUserDO> page, ImUserQO qo) {
        // 构建ES查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // name模糊查询（对应MySQL LIKE '%name%'）
        if (qo.getName() != null && !qo.getName().isBlank()) {
            boolQuery.must(QueryBuilders.wildcardQuery("name", "*" + qo.getName() + "*"));
        }
        // type = 0
        boolQuery.must(QueryBuilders.termQuery("type", 0));

        boolQuery.mustNot(QueryBuilders.termQuery("id", 1));

        // 构建查询
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of((int) page.getCurrent() - 1, (int) page.getSize()))
                .withSort(SortBuilders.fieldSort("gmtCreated").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("gmtUpdated").order(SortOrder.DESC));

        return getImUserDOPage(page, queryBuilder);
    }

    @NotNull
    private Page<ImUserDO> getImUserDOPage(IPage<ImUserDO> page, NativeSearchQueryBuilder queryBuilder) {
        NativeSearchQuery searchQuery = queryBuilder.build();

        // 执行查询
        SearchHits<ImUserES> searchHits = template.search(searchQuery, ImUserES.class);

        // 转换结果
        List<ImUserDO> records = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::convertToDO)
                .collect(Collectors.toList());

        // 构建分页结果
        Page<ImUserDO> result = new Page<>(page.getCurrent(), page.getSize());
        result.setRecords(records);
        result.setTotal(searchHits.getTotalHits());
        return result;
    }

    /**
     * 将ES实体转换为DO实体
     */
    private ImUserDO convertToDO(ImUserES es) {
        ImUserDO dto = new ImUserDO();
        BeanUtils.copyProperties(es, dto);
        return dto;
    }

    /**
     * 将ES实体转换为DTO实体
     */
    private ImUserDto convertToDto(ImUserES es) {
        ImUserDto dto = new ImUserDto();
        BeanUtils.copyProperties(es, dto);
        return dto;
    }

    /**
     * 将DO实体转换为ES实体
     */
    private ImUserES convertToEs(ImUserDO dto) {
        ImUserES es = new ImUserES();
        BeanUtils.copyProperties(dto, es);
        return es;
    }

    /**
     * 将DO实体转换为ES实体
     */
    private List<ImUserES> convertToEs(List<ImUserDO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) {
            return Collections.emptyList();
        }
        List<ImUserES> result = new ArrayList<>();
        for (ImUserDO dto : dtos) {
            result.add(convertToEs(dto));
        }
        return result;
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            Query query = new NativeSearchQuery(QueryBuilders.idsQuery().addIds(ids.stream().map(String::valueOf)
                    .toArray(String[]::new)));
            template.delete(query, ImUserES.class);
        }
    }

    @Override
    public void insert(List<ImUserDO> insertedUsers) {
        if (CollectionUtils.isNotEmpty(insertedUsers)) {
            List<ImUserES> imUserESList = convertToEs(insertedUsers);
            template.save(imUserESList);
        }
    }

    @Override
    public void update(ImUserDO userDO) {
        updateByIds(Collections.singletonList(userDO));
    }

    @Override
    public void updateByIds(List<ImUserDO> userDOS) {
        if (CollectionUtils.isNotEmpty(userDOS)) {
            IndexCoordinates index = template.getIndexCoordinatesFor(ImUserES.class);
            MappingElasticsearchConverter converter = (MappingElasticsearchConverter) template.getElasticsearchConverter();
            List<ImUserES> imUserESList = convertToEs(userDOS);

            List<UpdateQuery> updateQueries = imUserESList.stream().map(entity -> {
                // 1. 把实体转成 Document
                Document document = Document.create();
                converter.write(entity, document);

                // 2. 获取 ID（必须实体有 id 字段）
                String id = document.getId();

                // 3. 构造 UpdateQuery（4.4.18 官方标准）
                return UpdateQuery.builder(id)
                        .withDocument(document)
                        .build();

            }).collect(Collectors.toList());

            // 执行批量更新
            template.bulkUpdate(updateQueries, index);
        }
    }

    @Override
    public List<Long> existIds(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return idList;
        }
        List<String> stringList = idList.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        return existIds(stringList, ImUserES.class);
    }

    @Override
    public ImUserDO findByIdCard(String idCard) {
        SearchHits<ImUserES> searchHits = getByIdCardEs(idCard);
        if (searchHits == null) {
            return null;
        }
        return convertToDO(searchHits.getSearchHits().get(0).getContent());
    }

    @Nullable
    private SearchHits<ImUserES> getByIdCardEs(String idCard) {
        // 构建ES查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termQuery("idCard", idCard));

        // 构建查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(boolQuery).build();

        // 执行查询
        SearchHits<ImUserES> searchHits = template.search(searchQuery, ImUserES.class);

        // 转换结果
        if (searchHits.isEmpty()) {
            return null;
        }
        return searchHits;
    }

    @Override
    public ImUserDto getByIdCard(String idCard) {
        SearchHits<ImUserES> searchHits = getByIdCardEs(idCard);
        if (searchHits == null) {
            return null;
        }
        return convertToDto(searchHits.getSearchHits().get(0).getContent());
    }

    public <T> List<Long> existIds(List<String> idList, Class<T> clazz) {
        if (idList == null || idList.isEmpty()) {
            return List.of();
        }

        // 只查ID，不查内容，性能极高
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.idsQuery().addIds(idList.toArray(new String[0])))
                .withFields("id")
                .withPageable(Pageable.ofSize(10000))
                .build();

        return template.search(query, clazz)
                .stream()
                .map(SearchHit::getId)
                .filter(Objects::nonNull)
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<ImUserDto> pageByDepartmentIds(List<Long> deptIdList, String keywords, Integer pageNum,
                                                     Integer pageSize) {
        if (deptIdList == null || deptIdList.isEmpty()) {
            return new PageResult<>(0L, Long.valueOf(pageNum), 0L, Long.valueOf(pageSize), new LinkedList<>());
        }

        // 构建ES查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // departmentId IN 查询
        boolQuery.must(QueryBuilders.termsQuery("departmentId", deptIdList));

        // keywords 模糊查询（name OR mobile OR idCard OR code）
        boolean hasKeywords = keywords != null && !keywords.isBlank();
        if (hasKeywords) {
            BoolQueryBuilder keywordsQuery = QueryBuilders.boolQuery();
            keywordsQuery.should(QueryBuilders.wildcardQuery("name", "*" + keywords + "*"));
            keywordsQuery.should(QueryBuilders.wildcardQuery("mobile", "*" + keywords + "*"));
            keywordsQuery.should(QueryBuilders.wildcardQuery("idCard", "*" + keywords + "*"));
            keywordsQuery.should(QueryBuilders.wildcardQuery("code", "*" + keywords + "*"));
            keywordsQuery.minimumShouldMatch(1); // 至少匹配一个
            boolQuery.must(keywordsQuery);
        }

        // 构建分页查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .withSort(SortBuilders.fieldSort("gmtUpdated").order(SortOrder.DESC))
                .withFields("id", "code", "name", "avatar", "gender", "status") // 只返回需要的字段
                .build();

        // 执行查询
        SearchHits<ImUserES> searchHits = template.search(searchQuery, ImUserES.class);

        // 转换结果
        List<ImUserDto> records = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // 构建分页结果
        return new PageResult<>(
                searchHits.getTotalHits(),
                pageNum.longValue(),
                (long) records.size(),
                (long) pageSize,
                records
        );
    }
}
