package com.matthewcasperson;

import com.matthewcasperson.domain.MicroserviceKeyValue;
import com.matthewcasperson.security.OpaqueUser;
import com.matthewcasperson.security.ValidateSecretDetails;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Initializer;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import com.yahoo.elide.security.checks.Check;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

import static com.matthewcasperson.MicroserviceController.BASE_URL;

/**
 * This is the service that applications can call to determine what application
 * was used to generate a quote.
 */
@RestController
@RequestMapping(BASE_URL)
public class MicroserviceController {

	protected static final String BASE_URL = "/microservice/1.0";

	@Autowired
	private EntityManagerFactory emf;

	@Autowired
	private Initializer initializer;

	/**
	 * Converts a plain map to a multivalued map
	 * 
	 * @param input The original map
	 * @return A MultivaluedMap constructed from the input
	 */
	private MultivaluedMap<String, String> fromMap(final Map<String, String> input) {
		return new MultivaluedHashMap<String, String>(input);
	}

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, value = { "/{entity}",
			"/{entity}/{id}/relationships/{entity2}", "/{entity}/{id}/{child}", "/{entity}/{id}" })
	@Transactional
	@ResponseBody
	public String jsonApiGet(@RequestParam final Map<String, String> allRequestParams,
			@QueryParam("secret") final String secret, final HttpServletRequest request) {
		ElideResponse response = null;
		try {
			final String restOfTheUrl = request.getRequestURI().replaceFirst(BASE_URL, "");

			final SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);

			final DataStore dataStore = new HibernateStore(sessionFactory);

			final AuditLogger logger = new Slf4jLogger();

			final Map<String, Class<? extends Check>> checks = new HashMap<>();
			checks.put("Client supplied secret", ValidateSecretDetails.class);
			final EntityDictionary dictionary = new EntityDictionary(checks);

			final Elide elide = new Elide.Builder(dataStore).withAuditLogger(logger).withEntityDictionary(dictionary)
					.build();
			dictionary.bindInitializer(initializer, MicroserviceKeyValue.class);

			final MultivaluedMap<String, String> params = fromMap(allRequestParams);

			response = elide.get(restOfTheUrl, params, new OpaqueUser(secret));

		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
		}
		System.out.println(response.getBody());
		return response.getBody();
	}
}
