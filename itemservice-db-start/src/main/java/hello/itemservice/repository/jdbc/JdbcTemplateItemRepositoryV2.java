package hello.itemservice.repository.jdbc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;

/**
 * NamedParameterJdbcTemplate
 * SqlParameterSource
 * - BeanPropertySqlParameterSource
 * - MapSqlParameterSource
 * Map
 */
@Slf4j
public class JdbcTemplateItemRepositoryV2 implements ItemRepository {

	//private final JdbcTemplate template;
	private final NamedParameterJdbcTemplate template;

	public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
		this.template = new NamedParameterJdbcTemplate(dataSource);
	}

	@Override
	public Item save(Item item) {
		String sql = "insert into item(item_name, price, quantity) values(:itemName, :price, :quantity)";

		// 매개변수로 들어간 객체의 필드네임과 sql 구문의 values의 매개변수 명을 매핑시켜 sql구문을 만들어낸다.
		SqlParameterSource param = new BeanPropertySqlParameterSource(item);

		KeyHolder keyHolder = new GeneratedKeyHolder();
		template.update(sql, param, keyHolder);

		long key = keyHolder.getKey().longValue();
		item.setId(key);
		return item;
	}

	@Override
	public void update(Long itemId, ItemUpdateDto updateParam) {
		String sql = "update item set item_name=:itemName, price=:price, quantity=:quantity where id=:id";
		SqlParameterSource param = new MapSqlParameterSource()
			.addValue("itemName", updateParam.getItemName())
			.addValue("price", updateParam.getPrice())
			.addValue("quantity", updateParam.getQuantity())
			.addValue("id", itemId);

		template.update(sql, param);
	}

	@Override
	public Optional<Item> findById(Long id) {
		String sql = "select id, item_name, price ,quantity from ITEM where id = :id";
		try {
			Map<String, Long> param = Map.of("id", id);
			Item item = template.queryForObject(sql, param, itemRowMapper());
			return Optional.of(item);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public List<Item> findAll(ItemSearchCond cond) {
		String itemName = cond.getItemName();
		Integer maxPrice = cond.getMaxPrice();

		SqlParameterSource param = new BeanPropertySqlParameterSource(cond);

		String sql = "select id, item_name, price, quantity from ITEM";

		//동적 쿼리 (문제의 동적쿼리 생성부, 컬럼이 늘어날 수록 기하급수적으로 늘어남)
		if (StringUtils.hasText(itemName) || maxPrice != null) {
			sql += " where";
		}
		boolean andFlag = false;
		if (StringUtils.hasText(itemName)) {
			sql += " item_name like concat('%',:itemName,'%')";
			andFlag = true;
		}
		if (maxPrice != null) {
			if (andFlag) {
				sql += " and";
			}
			sql += " price <= :maxPrice";
		}
		log.info("sql={}", sql);
		return template.query(sql, param, itemRowMapper());
	}

	private RowMapper<Item> itemRowMapper() {
		return BeanPropertyRowMapper.newInstance(Item.class);
	}

}
