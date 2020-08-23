package com.boudata;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.LoginCustomizerFactory;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.cloud.entity.ModuleUser;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.modules.param.ParamService;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.permission.PermissionService;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IResourceService;
import com.boubei.tss.um.service.IRoleService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.XMLDocUtil;
import com.boudata.wms.WMS;

/**
 * create table scm.tx_company_code as select * from tx.tx_company_code
 *
 */
@ContextConfiguration(
	  locations={
			"classpath:META-INF/spring-framework.xml",
			"classpath:META-INF/spring-um.xml",
		    "classpath:META-INF/spring-mvc.xml",
		    "classpath:META-INF/spring-test.xml"
	  }
) 
@TransactionConfiguration(defaultRollback = true) // 自动回滚设置为false，否则数据将不插进去 true=回滚 false=插入
public abstract class AbstractTest4 extends AbstractTransactionalJUnit4SpringContextTests { 
 
    protected static Logger log = Logger.getLogger(AbstractTest4.class);    
    
    @Autowired protected IResourceService resourceService;
    @Autowired protected ILoginService loginSerivce;
    @Autowired protected APIService apiService;
    @Autowired protected PermissionService permissionService;
    @Autowired protected PermissionHelper permissionHelper;
    @Autowired protected LogService logService;
    @Autowired protected ParamService paramService;
    @Autowired protected ICommonDao commonDao;
    @Autowired protected ICommonService commonSerice;
    
    @Autowired protected IRoleService roleService;
    @Autowired protected IUserService userService;
    @Autowired protected IGroupService groupService;
    @Autowired protected ReportService reportService;
    @Autowired protected RecordService recordService;
    
    @Autowired protected WMS cx;
    
    public static Long ROLE_MG_ID = 2L;
	public static Long ROLE_OW_ID = 3L;
	public static Long ROLE_CG_ID = 4L;
	public static Long ROLE_OP_ID = 5L;
	public static Long ROLE_OT_ID = 7L;
	public static Long ROLE_SP_ID = 13L;
	public static Long ROLE_FIN_ID = 12L;
	public static Long ROLE_SELLER_ID = 13L;
	public static Long ROLE_BUYER_ID  = 14L;
    
    protected MockHttpServletRequest request;
    protected MockHttpServletResponse response;
    
    // 必须要先有个域登录
 	protected String domain = "BD";
 	protected Group domainGroup;
 	protected User domainUser;
 	protected User pm;
 	protected User cg;
 	protected User worker1;
 	protected User ow1;
 	protected User ow2;
 	protected User supplier1;
 	
 	protected Group customerGroup;
    
    @Before
    public void setUp() throws Exception {
    	
        Global.setContext(super.applicationContext);
        Context.setResponse(response = new MockHttpServletResponse());
		Context.initRequestContext(request = new MockHttpServletRequest());
        
        init();
    }
    
    private void init() {
		/*
		 * 初始化数据库脚本。 
		 * 此处直接通过jdbc（ stmt.execute(sql) ）向H2插入了初始数据，没法在spring-test框架里自动回滚。
		 * 通过hibernate生成的数据能回滚，因其事务由spring-test控制。
		 */
		if (paramService.getParam(0L) == null) {
			String sqlpath = _TestUtil.getInitSQLDir();
			log.info(" sql path : " + sqlpath);
			_TestUtil.excuteSQL(sqlpath);
			_TestUtil.excuteSQL(sqlpath + "/um");
		}
		
		/* 初始化应用系统、资源、权限项 */
		Document doc = XMLDocUtil.createDocByAbsolutePath(_TestUtil.getSQLDir() + "/tss-resource-config.xml");
		resourceService.applicationResourceRegister(doc, UMConstants.PLATFORM_SYSTEM_APP);

		// 初始化虚拟登录用户信息
		login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER);

		if (commonDao.getEntities("from Group where name=?", domain).isEmpty()) {
			// 企业注册
			domainUser = new User();
			domainUser.setLoginName(domain);
			domainUser.setUserName(domain);
			domainUser.setPassword("123456");
			
			userService.regBusiness(domainUser, domain, null);
			domainGroup = (Group) commonDao.getEntities("from Group where name=?", domain).get(0);
			customerGroup = (Group) commonDao.getEntities("from Group where name=?", "customer").get(0);

			SubAuthorize sa = new SubAuthorize();
			sa.setName("SA001");
			sa.setModuleId(999L);
			sa.setOwnerId(domainUser.getId());
			sa.setBuyerId(domainUser.getId());
			sa.setEndDate( DateUtil.addDays(100) );
			commonDao.create(sa);
			
			ModuleUser mu = new ModuleUser();
			mu.setModuleId(999L);
			mu.setUserId(domainUser.getId());
			mu.setDomain(domain);
			commonDao.create(mu);
			
			ROLE_SELLER_ID = createRole(WMS.ROLE_SELLER, domainUser.getId() + "").getId();
			
			// 新增用户
			pm = createUser("PM", domainGroup.getId(), ROLE_SELLER_ID);
			cg = createUser("CG", domainGroup.getId(), ROLE_SELLER_ID);
			worker1 = createUser("KF", domainGroup.getId(), ROLE_SELLER_ID);
			ow1 = createUser("OW-1", customerGroup.getId(), null);
			ow2 = createUser("OW-2", customerGroup.getId(), null);
			supplier1 = createUser("SP-1", domainGroup.getId(), null);
			
			// 新建角色
			ROLE_MG_ID = createRole(WMS.ROLE_MG, pm.getId() + "").getId();
			ROLE_OW_ID = createRole(WMS.ROLE_OW, ow1.getId() + "," + ow2.getId()).getId();
			ROLE_CG_ID = createRole(WMS.ROLE_CG, cg.getId() + "").getId();
			ROLE_OP_ID = createRole(WMS.ROLE_OP, worker1.getId() + "").getId();
			ROLE_OT_ID = createRole(WMS.ROLE_OT, "").getId();
			ROLE_SP_ID = createRole(WMS.ROLE_SP, supplier1.getId() + "").getId();
			ROLE_FIN_ID = createRole(WMS.ROLE_FIN, pm.getId() + "").getId();
			ROLE_BUYER_ID = createRole(WMS.ROLE_BUYER, "").getId();
			
			cx.init();
		}

		// 初始化虚拟登录用户信息
		login(worker1);
	}
    
    protected void login(User user) {
		logout();
		login(user.getId(), user.getLoginName());
	}
    
    protected void login(String loginName) {
    	login(loginSerivce.getOperatorDTOByLoginName(loginName).getId(), loginName);
    }

	protected void login(Long userId, String loginName) {
		LoginCustomizerFactory.customizer = null;

		apiService.mockLogin(loginName);
	}

	// 切换为匿名用户登陆，
	protected void logout() {
		String token = TokenUtil.createToken("1234567890", UMConstants.ANONYMOUS_USER_ID);
		Context.destroyIdentityCard(token);
		login(UMConstants.ANONYMOUS_USER_ID, Anonymous.one.getLoginName());
	}
    
    protected User createUser(String name, Long groupId, Long roleId) {
		User u = new User();
        u.setLoginName(name);
        u.setUserName(name);
        u.setPassword("123456");
        u.setGroupId(groupId);
        userService.createOrUpdateUser(u , groupId+"", EasyUtils.obj2String(roleId));
        return u;
	}
    
    protected Group createGroup(String name, Long parentId) {
		Group g = new Group();
        g.setParentId(parentId);
        g.setName(name);
        g.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(g , "", "");
        return g;
	}

    protected Role createRole(String name, Object userId) {
		Role r = new Role();
        r.setIsGroup(0);
        r.setName(name);
        r.setParentId(UMConstants.ROLE_ROOT_ID);
        r.setStartDate(DateUtil.today());
        r.setEndDate(DateUtil.addYears(50));
        r.setDisabled(0);
        
        roleService.saveRole2UserAndRole2Group(r, EasyUtils.obj2String(userId), "");
        return r;
	}
    
	public static void excuteSQLFile(String sqlDir) {  
        log.info("正在执行目录：" + sqlDir+ "下的SQL脚本。。。。。。");  
        
        Pool connePool = JCache.getInstance().getConnectionPool();
		Cacheable connItem = connePool.checkOut(0);
		
        try {  
        	Connection conn = (Connection) connItem.getValue();
            Statement stmt = conn.createStatement();  
            
            List<File> sqlFiles = FileHelper.listFilesByTypeDeeply(".sql", new File(sqlDir));
            for(File sqlFile : sqlFiles) {
            	String fileName = sqlFile.getName();

            	log.info("开始执行SQL脚本：" + fileName+ "。");  
            	
                String sqls = FileHelper.readFile(sqlFile, "UTF-8");
                String[] sqlArray = sqls.split(";");
                for(String sql : sqlArray) {
                	if( EasyUtils.isNullOrEmpty(sql) ) continue;
                	
                	log.debug(sql);  
                	stmt.execute(sql);
                }
				
                log.info("SQL脚本：" + fileName+ " 执行完毕。");  
            }
 
            log.info("成功执行目录：" + sqlDir+ "下的SQL脚本!");
            stmt.close(); 
            
        } catch (Exception e) {  
            throw new RuntimeException("目录：" + sqlDir+ "下的SQL脚本执行出错：", e);
        } finally {
        	connePool.checkIn(connItem);
        }
    }
}
