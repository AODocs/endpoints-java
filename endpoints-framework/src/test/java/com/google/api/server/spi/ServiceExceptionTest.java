package com.google.api.server.spi;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.UnauthorizedException;

import java.util.Map;
import java.util.logging.Level;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ServiceExceptionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testWithLogLevel() {
    UnauthorizedException ex = new UnauthorizedException("");
    assertThat(ex.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(ServiceException.withLogLevel(ex, Level.WARNING).getLogLevel())
        .isEqualTo(Level.WARNING);
  }

  @Test
  public void testExtraFields() {
    UnauthorizedException ex = new UnauthorizedException("");
    ex.addExtraField("isAdmin", TRUE)
      .addExtraField("userId", Integer.valueOf(12))
      .addExtraField("userName", "John Doe");
    Map<String, Object> extraFields = ex.getExtraFields();
    assertThat(extraFields.size()).isEqualTo(3);
    assertThat(extraFields.get("isAdmin")).isEqualTo(TRUE);
    assertThat(extraFields.get("userId")).isEqualTo(12);
    assertThat(extraFields.get("userName")).isEqualTo("John Doe");
  }

  @Test(expected = NullPointerException.class)
  public void testExtraFields_keyNull() {
    new BadRequestException("").addExtraField(null, "value not null");
  }

  @Test
  public void testExtraFields_valueNull_allowed() {
    UnauthorizedException ex = new UnauthorizedException("");
    ex.addExtraField("isAdmin", (String) null);
    Map<String, Object> extraFields = ex.getExtraFields();
    assertThat(extraFields.size()).isEqualTo(1);
    assertThat(extraFields.get("isAdmin")).isNull();
  }

  @Test
  public void testExtraFields_overrideValue_keepLast() {
    UnauthorizedException ex = new UnauthorizedException("");
    ex.addExtraField("isAdmin", FALSE);
    ex.addExtraField("isAdmin", TRUE);
    Map<String, Object> extraFields = ex.getExtraFields();
    assertThat(extraFields.size()).isEqualTo(1);
    assertThat(extraFields.get("isAdmin")).isEqualTo(TRUE);
  }

  @Test
  public void testExtraFields_ReservedKeyDomain_forbidden() {
    assertExtraFields_ReservedKeyword_forbidden("domain");
  }

  @Test
  public void testExtraFields_ReservedKeyMessage_forbidden() {
    assertExtraFields_ReservedKeyword_forbidden("message");
  }

  @Test
  public void testExtraFields_ReservedKeyReason_forbidden() {
    assertExtraFields_ReservedKeyword_forbidden("reason");
  }

  private void assertExtraFields_ReservedKeyword_forbidden(String keyword) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The keyword '" + keyword + "' is reserved");

    new ConflictException("Fails", "no extra " + keyword).addExtraField(keyword, "some other " + keyword);
  }
}
