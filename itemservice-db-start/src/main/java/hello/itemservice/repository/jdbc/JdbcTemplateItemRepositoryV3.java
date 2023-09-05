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
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.util.StringUtils;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;

/**
 * SimpleJdbcInsert - 이름대로 insert에서만 사용되는 기능
 */
@Slf4j
public class JdbcTemplateItemRepositoryV3 implements ItemRepository {

	//private final JdbcTemplate template;
	private final NamedParameterJdbcTemplate template;
	private final SimpleJdbcInsert jdbcInsert;

	public JdbcTemplateItemRepositoryV3(DataSource dataSource) {
		this.template = new NamedParameterJdbcTemplate(dataSource);
		this.jdbcInsert = new SimpleJdbcInsert(dataSource)
			.withTableName("item")
			.usingGeneratedKeyColumns("id");
		//.usingColumns("item_name", "price", "quantity"); // 생략 가능(item이라는 테이블을 찾아서 해당 테이블의 컬럼을 자동으로 인식함)
	}

	@Override
	public Item save(Item item) {
		SqlParameterSource param = new BeanPropertySqlParameterSource(item);
		Number key = jdbcInsert.executeAndReturnKey(param);
		item.setId(key.longValue());
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

		//동적 쿼리
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
		// rs.getString("item_name") 등을 자동으로 처리해줌
		// Item클래스을 해당 필드에 대한 필드명은 itemName이지만,
		// 관례의 불일치를 이미 알고 있기 때문에 BeanPropertyRowMapper가 camelCase와 snakeCase을 알아서 맞바꿔줌
		// 이름이 완전히 다르다면 sql문에 as라는 키워드를 통해 객체의 필드명과 똑같도록 별칭을 설정하여 이용
		return BeanPropertyRowMapper.newInstance(Item.class);
	}

}
