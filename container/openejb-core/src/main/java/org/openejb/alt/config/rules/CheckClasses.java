package org.openejb.alt.config.rules;

import org.openejb.OpenEJBException;
import org.openejb.alt.config.Bean;
import org.openejb.alt.config.EjbSet;
import org.openejb.alt.config.ValidationFailure;
import org.openejb.alt.config.ValidationRule;
import org.openejb.util.SafeToolkit;

import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;

public class CheckClasses implements ValidationRule {

    private EjbSet set;
    private ClassLoader classLoader;

    public void validate(EjbSet set) {
        this.set = set;

        Bean[] beans = set.getBeans();
        Bean b = null;
        try {
            for (int i = 0; i < beans.length; i++) {
                b = beans[i];
                check_hasEjbClass(b);
                check_isEjbClass(b);
                if (b.getHome() != null) {
                    check_hasHomeClass(b);
                    check_hasRemoteClass(b);
                    check_isHomeInterface(b);
                    check_isRemoteInterface(b);
                }
                if (b.getLocalHome() != null) {
                    check_hasLocalHomeClass(b);
                    check_hasLocalClass(b);
                    check_isLocalHomeInterface(b);
                    check_isLocalInterface(b);
                }
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(b.getEjbName(), e);
        }
    }

    private void check_hasLocalClass(Bean b) {
        lookForClass(b, b.getLocal(), "<local>");
    }

    private void check_hasLocalHomeClass(Bean b) {
        lookForClass(b, b.getLocalHome(), "<local-home>");
    }

    public void check_hasEjbClass(Bean b) {

        lookForClass(b, b.getEjbClass(), "<ejb-class>");

    }

    public void check_hasHomeClass(Bean b) {

        lookForClass(b, b.getHome(), "<home>");

    }

    public void check_hasRemoteClass(Bean b) {

        lookForClass(b, b.getRemote(), "<remote>");

    }

    public void check_isEjbClass(Bean b) {

        if (b instanceof org.openejb.alt.config.SessionBean) {

            compareTypes(b, b.getEjbClass(), javax.ejb.SessionBean.class);

        } else if (b instanceof org.openejb.alt.config.EntityBean) {

            compareTypes(b, b.getEjbClass(), javax.ejb.EntityBean.class);

        }

    }

    private void check_isLocalInterface(Bean b) {
        compareTypes(b, b.getLocal(), EJBLocalObject.class);
    }

    private void check_isLocalHomeInterface(Bean b) {
        compareTypes(b, b.getLocalHome(), EJBLocalHome.class);
    }

    public void check_isHomeInterface(Bean b) {

        compareTypes(b, b.getHome(), javax.ejb.EJBHome.class);

    }

    public void check_isRemoteInterface(Bean b) {

        compareTypes(b, b.getRemote(), javax.ejb.EJBObject.class);

    }

    private void lookForClass(Bean b, String clazz, String type) {
        try {
            loadClass(clazz);
        } catch (OpenEJBException e) {
            /*
            # 0 - Class name
            # 1 - Element (home, ejb-class, remote)
            # 2 - Bean name
            */

            ValidationFailure failure = new ValidationFailure("missing.class");
            failure.setDetails(clazz, type, b.getEjbName());
            failure.setBean(b);

            set.addFailure(failure);

        } catch (NoClassDefFoundError e) {
            /*
             # 0 - Class name
             # 1 - Element (home, ejb-class, remote)
             # 2 - Bean name
             # 3 - Misslocated Class name
             */
            ValidationFailure failure = new ValidationFailure("misslocated.class");
            failure.setDetails(clazz, type, b.getEjbName(), e.getMessage());
            failure.setBean(b);

            set.addFailure(failure);
            throw e;
        }

    }

    private void compareTypes(Bean b, String clazz1, Class class2) {
        Class class1 = null;
        try {
            class1 = loadClass(clazz1);
        } catch (OpenEJBException e) {
            return;
        }

        if (class1 != null && !class2.isAssignableFrom(class1)) {
            ValidationFailure failure = new ValidationFailure("wrong.class.type");
            failure.setDetails(clazz1, class2.getName());
            failure.setBean(b);

            set.addFailure(failure);

        }
    }

    private Class loadClass(String clazz) throws OpenEJBException {
        ClassLoader cl = set.getClassLoader();
        try {
            return cl.loadClass(clazz);
        } catch (ClassNotFoundException cnfe) {
            throw new OpenEJBException(SafeToolkit.messages.format("cl0007", clazz, set.getJarPath()));
        }
    }

}

