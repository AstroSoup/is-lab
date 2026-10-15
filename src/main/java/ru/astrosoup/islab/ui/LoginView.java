package ru.astrosoup.islab.ui;

import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ru.astrosoup.islab.services.AuthService;
import ru.astrosoup.islab.ui.util.Notifications;
import ru.astrosoup.islab.ui.util.UiErrors;

@RouteScoped
@Route("login")
@PageTitle("Вход")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    AuthService auth;

    private final TextField name = new TextField("Имя пользователя");
    private final PasswordField password = new PasswordField("Пароль");

    @PostConstruct
    void init() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        name.setMaxLength(AuthService.MAX_NAME);
        name.setWidth("320px");
        password.setWidth("320px");

        Button login = new Button("Войти", e -> login());
        login.addThemeName("primary");
        login.addClickShortcut(Key.ENTER);
        Button register = new Button("Зарегистрироваться", e -> register());

        add(new H2("Информационная система «Организации»"), name, password,
                new HorizontalLayout(login, register));
    }

    private void login() {
        try {
            if (auth.authenticate(name.getValue(), password.getValue())) {
                UserSession.login(name.getValue().trim());
                UI.getCurrent().navigate(OrganizationsView.class);
            } else {
                Notifications.error("Неверное имя пользователя или пароль");
            }
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
        }
    }

    private void register() {
        try {
            auth.register(name.getValue(), password.getValue());
            Notifications.success("Пользователь зарегистрирован, теперь можно войти");
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (UserSession.isLoggedIn()) {
            event.forwardTo(OrganizationsView.class);
        }
    }
}
