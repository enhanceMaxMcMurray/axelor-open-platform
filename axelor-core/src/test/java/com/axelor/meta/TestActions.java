/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.actions.validate.validator.Info;
import com.axelor.meta.schema.views.FormView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestActions extends MetaTest {

  private ObjectViews views;

  @Inject private ContactRepository contacts;

  @Inject private ActionExecutor executor;

  @BeforeEach
  public void setUp() {
    try {
      views = this.unmarshal("com/axelor/meta/Contact.xml", ObjectViews.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    assertNotNull(views);
    assertNotNull(views.getActions());

    MetaStore.resister(views);
  }

  private ActionHandler createHandler(String action, Map<String, Object> context) {

    ActionRequest request = new ActionRequest();

    Map<String, Object> data = Maps.newHashMap();
    request.setData(data);
    request.setModel("com.axelor.test.db.Contact");
    request.setAction(action);

    data.put("context", context);

    return executor.newActionHandler(request);
  }

  private ActionHandler createHandler(Action action, Map<String, Object> context) {
    Preconditions.checkArgument(action != null, "action is null");
    return createHandler(action.getName(), context);
  }

  @Test
  public void testRecord() {

    Action action = MetaStore.getAction("action-contact-defaults");
    ActionHandler handler = createHandler(action, null);

    Object value = action.execute(handler);
    assertTrue(value instanceof Contact);

    Contact c = (Contact) value;

    assertNotNull(c.getTitle());
    assertEquals("Mr. John Smith", c.getFullName());
  }

  @Test
  public void testMultiRecord() {

    Action action = MetaStore.getAction("action-contact-defaults-multi");
    ActionHandler handler = createHandler(action, null);

    Object value = action.execute(handler);
    assertTrue(value instanceof Contact);

    Contact c = (Contact) value;

    assertNotNull(c.getLastName());
    assertNotNull(c.getFirstName());
    assertEquals(c.getFirstName(), c.getLastName());
    assertEquals("Smith", c.getLastName());
    assertEquals("Mr. Smith Smith", c.getFullName());

    assertNotNull(c.getEmail());
    assertNotNull(c.getProEmail());
    assertEquals(c.getProEmail(), c.getEmail());
    assertEquals("john.smith@gmail.com", c.getEmail());
  }

  @Test
  public void testAttrs() {
    Action action = MetaStore.getAction("action-contact-attrs");
    ActionHandler handler = createHandler(action, null);

    Object value = action.execute(handler);
    assertTrue(value instanceof Map);

    Map<?, ?> map = (Map<?, ?>) value;
    Map<?, ?> attrs = (Map<?, ?>) map.get("lastName");

    assertTrue(attrs instanceof Map);
    assertEquals(true, attrs.get("readonly"));
    assertEquals(true, attrs.get("hidden"));

    attrs = (Map<?, ?>) map.get("notes");

    assertTrue(attrs instanceof Map);
  }

  @Test
  public void testAttrsMutli() {
    Action action = MetaStore.getAction("action-contact-attrs-multi");
    ActionHandler handler = createHandler(action, null);

    Object value = action.execute(handler);
    assertTrue(value instanceof Map);

    Map<?, ?> map = (Map<?, ?>) value;
    Map<?, ?> attrs = (Map<?, ?>) map.get("lastName");

    assertTrue(attrs instanceof Map);
    assertEquals(true, attrs.get("readonly"));
    assertEquals(true, attrs.get("hidden"));

    attrs = (Map<?, ?>) map.get("notes");

    assertTrue(attrs instanceof Map);
    assertEquals("About Me", attrs.get("title"));

    Map<?, ?> attrsPhone = (Map<?, ?>) map.get("phone");
    Map<?, ?> attrsNotes = (Map<?, ?>) map.get("notes");
    Map<?, ?> attrsBirth = (Map<?, ?>) map.get("dateOfBirth");

    assertTrue(attrs instanceof Map);
    assertEquals(true, attrsPhone.get("hidden"));
    assertEquals(attrsPhone.get("hidden"), attrsNotes.get("hidden"));
    assertEquals(attrsBirth.get("hidden"), attrsNotes.get("hidden"));

    Map<?, ?> attrsFisrtName = (Map<?, ?>) map.get("firstName");
    Map<?, ?> attrsLastName = (Map<?, ?>) map.get("lastName");

    assertTrue(attrs instanceof Map);
    assertEquals(true, attrsFisrtName.get("readonly"));
    assertEquals(attrsFisrtName.get("readonly"), attrsLastName.get("readonly"));
    assertEquals(true, attrsLastName.get("hidden"));
  }

  @Test
  public void testValidate() {

    Action action = MetaStore.getAction("action-contact-validate");
    Map<String, Object> context = Maps.newHashMap();

    context.put("id", 1);
    context.put("firstName", "John");
    context.put("lastName", "Sm");

    ActionHandler handler = createHandler(action, context);
    Object value = action.execute(handler);

    assertNotNull(value);
  }

  @Test
  public void testCondition() {

    Action action = MetaStore.getAction("check.dates");
    Map<String, Object> context = Maps.newHashMap();

    context.put("orderDate", LocalDate.parse("2012-12-10"));
    context.put("createDate", LocalDate.parse("2012-12-11"));

    ActionHandler handler = createHandler(action, context);
    Object value = action.execute(handler);

    assertNotNull(value);
    assertTrue(value instanceof Map);
    assertTrue(!((Map<?, ?>) value).isEmpty());
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testMethod() {

    Action action = MetaStore.getAction("action-contact-greetings");
    Map<String, Object> context = Maps.newHashMap();

    context.put("id", 1);
    context.put("firstName", "John");
    context.put("lastName", "Smith");

    ActionHandler handler = createHandler(action, context);
    Object value = action.execute(handler);

    assertNotNull(value);
    Map infoMap =
        (Map) ((Map<?, ?>) ((List<?>) ((ActionResponse) value).getData()).get(0)).get(Info.KEY);
    assertNotNull(infoMap);
    assertEquals("Hello World!!!", infoMap.get("message"));
    assertEquals("My title", infoMap.get("title"));
  }

  @Test
  public void testRpc() {

    Action action = MetaStore.getAction("action-contact-greetings-rpc");
    Map<String, Object> context = Maps.newHashMap();

    context.put("id", 1);
    context.put("firstName", "John");
    context.put("lastName", "Smith");
    context.put("fullName", "John Smith");

    ActionHandler handler = createHandler(action, context);
    Object value = action.execute(handler);

    assertNotNull(value);
    assertEquals("Say: John Smith", value);

    value = handler.evaluate("call: com.axelor.meta.web.Hello:say(fullName)");

    assertNotNull(value);
    assertEquals("Say: John Smith", value);

    try {
      handler.evaluate("call: com.axelor.meta.web.Hello:unauthorizedCallMethod(fullName)");
      fail("Calling non rpc methods without @CallMethod annotation are not allowed");
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
  }

  @Test
  public void testEvents() throws Exception {

    FormView view = (FormView) views.getViews().get(1);
    assertNotNull(view);

    Map<String, Object> context = Maps.newHashMap();

    context.put("firstName", "John");
    context.put("lastName", "Smith");

    String onLoad = view.getOnLoad();
    String onSave = view.getOnSave();

    ActionHandler handler = createHandler(onLoad, context);
    ActionResponse response = handler.execute();
    assertNotNull(response.getData());

    handler = createHandler(onSave, context);
    response = handler.execute();
    assertNotNull(response.getData());
  }

  @Test
  public void testView() {

    Action action = MetaStore.getAction("action-view-contact");
    Map<String, Object> context = Maps.newHashMap();

    context.put("id", 1);
    context.put("firstName", "John");
    context.put("lastName", "Smith");

    ActionHandler handler = createHandler(action, context);
    Object value = action.execute(handler);

    assertNotNull(value);
  }

  @Test
  public void testGroup() {
    Action action = MetaStore.getAction("action.group.test");
    Map<String, Object> context = Maps.newHashMap();

    context.put("id", 1);
    context.put("firstName", "John");
    context.put("lastName", "Smith");

    ActionHandler handler = createHandler(action, context);
    Object value = action.execute(handler);

    assertNotNull(value);
    assertTrue(value instanceof List);
    assertFalse(((List<?>) value).isEmpty());
    assertNotNull(((List<?>) value).get(0));
    assertFalse(value.toString().contains("pending"));

    handler.getContext().put("firstName", "J");
    handler.getContext().put("email", "j.smith@gmail.com");

    value = action.execute(handler);

    assertNotNull(value);
    assertTrue(value instanceof List);
    assertFalse(((List<?>) value).isEmpty());
    assertNotNull(((List<?>) value).get(0));
    assertTrue(value.toString().contains("pending"));
  }
}
