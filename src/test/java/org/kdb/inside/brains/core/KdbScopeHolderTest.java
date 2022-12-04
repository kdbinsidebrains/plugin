package org.kdb.inside.brains.core;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Ignore;

@Ignore
public class KdbScopeHolderTest extends BasePlatformTestCase {
/*    private KdbScopeHolder holder;
    private PasswordSafe passwordSafe;

    private final Disposable disposable = Disposer.newDisposable();

    @BeforeEach
    void init() {
        holder = new KdbScopeHolder();

        passwordSafe = mock(PasswordSafe.class);

        final MockApplication app = new MockApplication(disposable);
        app.registerService(PasswordSafe.class, passwordSafe);
        ApplicationManager.setApplication(app, disposable);
    }

    @AfterEach
    void clean() {
        disposable.dispose();
    }

    @Test
    void onlyScope() {
        final KdbScope s = new KdbScope("Scope1", ScopeType.SHARED);
        holder.addScope(s);

        final var state = holder.getState();

        XMLOutputter outp = new XMLOutputter();
        assertEquals("<scopes><scope name=\"Scope1\" /></scopes>", outp.outputString(state));

        verify(passwordSafe).setPassword(KdbScopeHolder.createAttribute("Scope1"), null);
        verifyNoMoreInteractions(passwordSafe);
    }

    @Test
    void complex() {
        final KdbScope scope = new KdbScope("Scope1", ScopeType.SHARED, "ScopeCredentials", new InstanceOptions(1231, true, true));
        holder.addScope(scope);

        final PackageItem p1 = scope.createPackage("p1");
        final PackageItem p2 = scope.createPackage("p2");
        final PackageItem p3 = p2.createPackage("p1");

        final KdbInstance i1 = scope.createInstance("i1", "localhost", 10, null, null);
        final KdbInstance i2 = p1.createInstance("i2", "localhost", 10, "adqqe", null);
        final KdbInstance i3 = p3.createInstance("i3", "localhost", 10, "dfgsdfgsdf", new InstanceOptions(123213, true, true));

        final Element state = holder.getState();

        XMLOutputter outp = new XMLOutputter();
        assertEquals("<scopes>" +
                "<scope name=\"Scope1\" timeout=\"1231\" tls=\"true\" compression=\"true\">" +
                "<package name=\"p1\">" +
                "<instance name=\"i2\" host=\"localhost\" port=\"10\" />" +
                "</package>" +
                "<package name=\"p2\">" +
                "<package name=\"p1\">" +
                "<instance name=\"i3\" host=\"localhost\" port=\"10\" timeout=\"123213\" tls=\"true\" compression=\"true\" />" +
                "</package>" +
                "</package>" +
                "<instance name=\"i1\" host=\"localhost\" port=\"10\" />" +
                "</scope>" +
                "</scopes>", outp.outputString(state));

        verify(passwordSafe).setPassword(KdbScopeHolder.createAttribute("Scope1"), "ScopeCredentials");
        verify(passwordSafe).setPassword(KdbScopeHolder.createAttribute("Scope1/i1[2]"), null);
        verify(passwordSafe).setPassword(KdbScopeHolder.createAttribute("Scope1/p1[0]/i2[0]"), "adqqe");
        verify(passwordSafe).setPassword(KdbScopeHolder.createAttribute("Scope1/p2[1]/p1[0]/i3[0]"), "dfgsdfgsdf");
        verifyNoMoreInteractions(passwordSafe);
    }

    @Test
    void encodeDecode() {
        final KdbScope scope = new KdbScope("Scope1", ScopeType.SHARED, "ScopeCredentials", new InstanceOptions(1231, true, true));
        holder.addScope(scope);

        final PackageItem p1 = scope.createPackage("p1");
        final PackageItem p2 = scope.createPackage("p2");
        final PackageItem p3 = p2.createPackage("p1");

        final KdbInstance i1 = scope.createInstance("i1", "localhost", 10, null, null);
        final KdbInstance i2 = p1.createInstance("i2", "localhost2", 20, "adqqe", null);
        final KdbInstance i3 = p3.createInstance("i3", "localhost3", 30, "dfgsdfgsdf", new InstanceOptions(123213, true, true));

        final Element state = holder.getState();

        reset(passwordSafe);
        when(passwordSafe.getPassword(KdbScopeHolder.createAttribute("Scope1"))).thenReturn("ScopeCredentials");
        when(passwordSafe.getPassword(KdbScopeHolder.createAttribute("Scope1/i1[0]"))).thenReturn(null);
        when(passwordSafe.getPassword(KdbScopeHolder.createAttribute("Scope1/p1[0]/i2[0]"))).thenReturn("adqqe");
        when(passwordSafe.getPassword(KdbScopeHolder.createAttribute("Scope1/p2[1]/p1[0]/i3[0]"))).thenReturn("dfgsdfgsdf");

        holder.loadState(state);

        final List<KdbScope> scopes = holder.getScopes();
        assertEquals(1, scopes.size());
        assertNotSame(scope, scopes.get(0));

        final KdbScope scope1 = scopes.get(0);
        assertEquals(scope.getName(), scope1.getName());
        assertEquals(scope.getCredentials(), scope1.getCredentials());
        assertEquals(scope.getOptions(), scope1.getOptions());

        assertEquals(3, scope1.getChildren().size());
        final PackageItem pp1 = (PackageItem) scope1.getChildren().get(0);
        final PackageItem pp2 = (PackageItem) scope1.getChildren().get(1);
        assertEquals("p1", pp1.getName());
        assertEquals("p2", pp2.getName());

        final KdbInstance inst1 = (KdbInstance) scope1.getChildren().get(2);
        assertEquals("i1", inst1.getName());
        assertEquals("localhost", inst1.getHost());
        assertEquals(10, inst1.getPort());
        assertNull(inst1.getCredentials());
        assertNull(inst1.getOptions());

        final KdbInstance inst2 = (KdbInstance) pp1.getChildren().get(0);
        assertEquals("i2", inst2.getName());
        assertEquals("localhost2", inst2.getHost());
        assertEquals(20, inst2.getPort());
        assertEquals("adqqe", inst2.getCredentials());
        assertNull(inst2.getOptions());

        final KdbInstance inst3 = (KdbInstance) ((PackageItem) pp2.getChildren().get(0)).getChildren().get(0);
        assertEquals("i3", inst3.getName());
        assertEquals("localhost3", inst3.getHost());
        assertEquals(30, inst3.getPort());
        assertEquals("dfgsdfgsdf", inst3.getCredentials());
        assertEquals(new InstanceOptions(123213, true, true), inst3.getOptions());
    }*/
}