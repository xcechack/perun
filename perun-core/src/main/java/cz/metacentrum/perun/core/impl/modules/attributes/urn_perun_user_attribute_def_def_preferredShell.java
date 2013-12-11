package cz.metacentrum.perun.core.impl.modules.attributes;

import java.util.List;

import cz.metacentrum.perun.core.api.Attribute;
import cz.metacentrum.perun.core.api.AttributeDefinition;
import cz.metacentrum.perun.core.api.AttributesManager;
import cz.metacentrum.perun.core.api.User;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeAssignmentException;
import cz.metacentrum.perun.core.api.exceptions.WrongAttributeValueException;
import cz.metacentrum.perun.core.api.exceptions.WrongReferenceAttributeValueException;
import cz.metacentrum.perun.core.impl.PerunSessionImpl;
import cz.metacentrum.perun.core.implApi.modules.attributes.UserAttributesModuleAbstract;
import cz.metacentrum.perun.core.implApi.modules.attributes.UserAttributesModuleImplApi;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Michal Šťava   <stava.michal@gmail.com>
 * @version $Id$
 */
public class urn_perun_user_attribute_def_def_preferredShell extends UserAttributesModuleAbstract implements UserAttributesModuleImplApi {

  public void checkAttributeValue(PerunSessionImpl sess, User user, Attribute attribute) throws InternalErrorException, WrongAttributeValueException, WrongAttributeAssignmentException, WrongReferenceAttributeValueException {
    String shell = (String) attribute.getValue();
    
    //Can be null, if not, need to check format
    if(shell != null && !shell.isEmpty()) {
        sess.getPerunBl().getModulesUtilsBl().checkFormatOfShell(shell, attribute);
    }
     
  }
  
  public AttributeDefinition getAttributeDefinition() {
      AttributeDefinition attr = new AttributeDefinition();
      attr.setNamespace(AttributesManager.NS_USER_ATTR_DEF);
      attr.setFriendlyName("preferredShell");
      attr.setType(String.class.getName());
      attr.setDescription("User preferred shell, choosed automatic if it is allowed.");
      return attr;
  }
}