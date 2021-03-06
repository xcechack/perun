package cz.metacentrum.perun.voot;

import cz.metacentrum.perun.core.api.*;
import cz.metacentrum.perun.core.api.exceptions.*;
import cz.metacentrum.perun.core.bl.PerunBl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

/**
 * Tests of VOOT protocol calls for subgroups.
 *
 * @author Martin Malik <374128@mail.muni.cz>
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:perun-voot-applicationcontext.xml","classpath:perun-beans.xml"})
@TransactionConfiguration(defaultRollback=true)
@Transactional

public class VOOTSubGroupsIntegrationTest {
	@Autowired
	private PerunBl perun;

	private PerunSession session;
	private VOOT voot;

	private Vo vo1;
	private Group group1;
	private Group group2; //group 2 is subgroup of group1
	private Group group3;

	private User user1;

	private Member member1;
	private Member member2;

	@Before
	public void setUpSession() throws Exception{
		session = perun.getPerunSession(new PerunPrincipal("perunTests", ExtSourcesManager.EXTSOURCE_NAME_INTERNAL, ExtSourcesManager.EXTSOURCE_INTERNAL));
		user1 = setUpUser1();
		setUpBackground();
		session.getPerunPrincipal().setUser(user1);
	}

	@Test
	public void isMemberOfSubGroupTest() throws InternalErrorException, AlreadyMemberException, WrongAttributeValueException, WrongReferenceAttributeValueException, NotMemberOfParentGroupException, VOOTException, GroupNotExistsException {
		System.out.println("IsMemberOfSubGroupTest");
		VOOT voot = new VOOT();
		Response response = (Response) voot.process(session, "groups/@me", "");
		assertEquals(3, response.getEntry().length);
		System.out.println(response);
	}

	@Test
	public void groupMembersSubGroupTest() throws VOOTException {
		System.out.println("groupMembersSubGroupTest");
		VOOT voot = new VOOT();
		Response response = (Response) voot.process(session, "people/@me/vo1:group1:group2", "");
		assertEquals(2, response.getEntry().length);
		System.out.println(response);
	}

	private void setUpBackground() throws VoExistsException, InternalErrorException, GroupExistsException, AlreadyMemberException, WrongAttributeValueException, WrongAttributeAssignmentException, WrongReferenceAttributeValueException, NotMemberOfParentGroupException, AlreadyAdminException, AttributeNotExistsException, ExtendMembershipException {
		vo1 = perun.getVosManagerBl().createVo(session, new Vo(1, "vo1", "vo1"));

		group1 = perun.getGroupsManagerBl().createGroup(session, vo1, new Group("group1", "group1 in vo1"));
		group2 = perun.getGroupsManagerBl().createGroup(session, group1, new Group("group2", "group2 is subgroup of group1"));

		member1 = perun.getMembersManagerBl().createMember(session, vo1, user1);

		User user2 = new User();
		user2.setFirstName("Karol");
		user2.setLastName("Druhy");
		user2 = perun.getUsersManagerBl().createUser(session, user2);
		member2 = perun.getMembersManagerBl().createMember(session, vo1, user2);

		perun.getGroupsManagerBl().addMember(session, group2, member1);
		perun.getGroupsManagerBl().addMember(session, group2, member2);
	}

	private User setUpUser1() throws InternalErrorException, WrongAttributeAssignmentException, WrongAttributeValueException, WrongReferenceAttributeValueException {
		User user = new User();
		user.setFirstName("James");
		user.setMiddleName("");
		user.setLastName("Bond");
		user.setTitleBefore("");
		user.setTitleAfter("");

		return perun.getUsersManagerBl().createUser(session, user);
	}
}
