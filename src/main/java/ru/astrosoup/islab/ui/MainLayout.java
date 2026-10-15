package ru.astrosoup.islab.ui;

import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

@RouteScoped 
public class MainLayout extends AppLayout {

    public MainLayout() {
        H1 title = new H1("Организации");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0 auto 0 0");

        Span user = new Span(UserSession.currentUser().orElse(""));
        Button logout = new Button("Выйти", VaadinIcon.SIGN_OUT.create(), e -> UserSession.logout());
        HorizontalLayout userBar = new HorizontalLayout(user, logout);
        userBar.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        userBar.getStyle().set("padding-right", "var(--lumo-space-m)");

        addToNavbar(new DrawerToggle(), title, userBar);

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Организации", OrganizationsView.class, VaadinIcon.TABLE.create()));
        nav.addItem(new SideNavItem("Координаты", CoordinatesView.class, VaadinIcon.MAP_MARKER.create()));
        nav.addItem(new SideNavItem("Адреса", AddressesView.class, VaadinIcon.HOME.create()));
        nav.addItem(new SideNavItem("Специальные операции", OperationsView.class, VaadinIcon.COGS.create()));
        addToDrawer(nav);
    }
}
