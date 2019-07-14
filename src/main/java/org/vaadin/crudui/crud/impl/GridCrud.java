package org.vaadin.crudui.crud.impl;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.provider.Query;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.vaadin.crudui.crud.*;
import org.vaadin.crudui.form.CrudFormFactory;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;
import org.vaadin.crudui.layout.CrudLayout;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import java.util.Collection;

import static lombok.AccessLevel.PROTECTED;

/**
 * @author Alejandro Duarte
 */
public class GridCrud<T> extends AbstractCrud<T> {

    @Getter(PROTECTED)
    @Setter
    private String rowCountCaption = "%d items(s) found";
    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private String savedMessage = "Item saved";
    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private String deletedMessage = "Item deleted";
    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private boolean showNotifications = true;

    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private Button findAllButton;
    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private Button addButton;
    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private Button updateButton;
    @Getter(PROTECTED)
    @Setter(PROTECTED)
    private Button deleteButton;
    @Getter
    @Setter(PROTECTED)
    private Grid<T> grid;

    private boolean autoCreateColumns;

    private boolean clickRowToUpdate;

    public GridCrud(Class<T> domainType) {
        this(domainType, true);
    }

    public GridCrud(Class<T> domainType, boolean autoCreateColumns) {
        this(domainType, autoCreateColumns, new WindowBasedCrudLayout(), new DefaultCrudFormFactory<>(domainType), null);
    }

    public GridCrud(Class<T> domainType, CrudLayout crudLayout) {
        this(domainType, true, crudLayout);
    }

    public GridCrud(Class<T> domainType, boolean autoCreateColumns, CrudLayout crudLayout) {
        this(domainType, autoCreateColumns, crudLayout, new DefaultCrudFormFactory<>(domainType), null);
    }

    public GridCrud(Class<T> domainType, CrudFormFactory<T> crudFormFactory) {
        this(domainType, new WindowBasedCrudLayout(), crudFormFactory, null);
    }

    public GridCrud(Class<T> domainType, CrudListener<T> crudListener) {
        this(domainType, new WindowBasedCrudLayout(), new DefaultCrudFormFactory<>(domainType), crudListener);
    }

    public GridCrud(Class<T> domainType, CrudLayout crudLayout, CrudFormFactory<T> crudFormFactory) {
        this(domainType, crudLayout, crudFormFactory, null);
    }

    public GridCrud(Class<T> domainType, CrudLayout crudLayout, CrudFormFactory<T> crudFormFactory, CrudListener<T> crudListener) {
        this(domainType, false, crudLayout, crudFormFactory, crudListener);
    }

    public GridCrud(Class<T> domainType,
                    boolean autoCreateColumns,
                    CrudLayout crudLayout,
                    CrudFormFactory<T> crudFormFactory,
                    CrudListener<T> crudListener) {
        super(domainType, crudLayout, crudFormFactory, crudListener);
        this.autoCreateColumns = autoCreateColumns;
        initLayout();
    }

    protected void initLayout() {
        findAllButton = new Button(VaadinIcon.REFRESH.create(), e -> findAllButtonClicked());
        findAllButton.getElement().setAttribute("title", "Refresh list");

        crudLayout.addToolbarComponent(findAllButton);

        addButton = new Button(VaadinIcon.PLUS.create(), e -> addButtonClicked());
        addButton.getElement().setAttribute("title", "Add");
        crudLayout.addToolbarComponent(addButton);

        updateButton = new Button(VaadinIcon.PENCIL.create(), e -> updateButtonClicked());
        updateButton.getElement().setAttribute("title", "Update");
        crudLayout.addToolbarComponent(updateButton);

        deleteButton = new Button(VaadinIcon.TRASH.create(), e -> deleteButtonClicked());
        deleteButton.getElement().setAttribute("title", "Delete");
        crudLayout.addToolbarComponent(deleteButton);

        grid = new Grid<>(domainType, autoCreateColumns);
        grid.setSizeFull();
        grid.addSelectionListener(e -> gridSelectionChanged());
        crudLayout.setMainComponent(grid);

        updateButtons();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshGrid();
    }

    @Override
    public void setAddOperationVisible(boolean visible) {
        addButton.setVisible(visible);
    }

    @Override
    public void setUpdateOperationVisible(boolean visible) {
        updateButton.setVisible(visible);
    }

    @Override
    public void setDeleteOperationVisible(boolean visible) {
        deleteButton.setVisible(visible);
    }

    @Override
    public void setFindAllOperationVisible(boolean visible) {
        findAllButton.setVisible(false);
    }

    public void refreshGrid() {
        if (LazyFindAllCrudOperationListener.class.isAssignableFrom(findAllOperation.getClass())) {
            LazyFindAllCrudOperationListener findAll = (LazyFindAllCrudOperationListener) findAllOperation;

            grid.setDataProvider(findAll.getDataProvider());

        } else {
            Collection<T> items = findAllOperation.findAll();
            grid.setItems(items);
        }
    }

    public void setClickRowToUpdate(boolean clickRowToUpdate) {
        this.clickRowToUpdate = clickRowToUpdate;
    }

    protected void updateButtons() {
        boolean rowSelected = !grid.asSingleSelect().isEmpty();
        updateButton.setEnabled(rowSelected);
        deleteButton.setEnabled(rowSelected);
    }

    protected void gridSelectionChanged() {
        updateButtons();
        T domainObject = grid.asSingleSelect().getValue();

        if (domainObject != null) {
            if (clickRowToUpdate) {
                updateButtonClicked();
            } else {
                Component form = crudFormFactory.buildNewForm(CrudOperation.READ, domainObject, true, null, event -> {
                    grid.asSingleSelect().clear();
                });
                String caption = crudFormFactory.buildCaption(CrudOperation.READ, domainObject);
                crudLayout.showForm(CrudOperation.READ, form, caption);
            }
        } else {
            crudLayout.hideForm();
        }
    }

    protected void findAllButtonClicked() {
        grid.asSingleSelect().clear();
        refreshGrid();
        showNotification(String.format(rowCountCaption, grid.getDataProvider().size(new Query())));
    }

    protected void addButtonClicked() {
        try {
            T domainObject = domainType.newInstance();
            showForm(CrudOperation.ADD, domainObject, false, savedMessage, event -> {
                try {
                    T addedObject = addOperation.perform(domainObject);
                    refreshGrid();
                    grid.asSingleSelect().setValue(addedObject);
                    // TODO: grid.scrollTo(addedObject);
                } catch (IllegalArgumentException ignore) {
                } catch (CrudOperationException e1) {
                    refreshGrid();
                } catch (Exception e2) {
                    refreshGrid();
                    throw e2;
                }
            });
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updateButtonClicked() {
        T domainObject = grid.asSingleSelect().getValue();
        showForm(CrudOperation.UPDATE, domainObject, false, savedMessage, event -> {
            try {
                T updatedObject = updateOperation.perform(domainObject);
                grid.asSingleSelect().clear();
                refreshGrid();
                grid.asSingleSelect().setValue(updatedObject);
                // TODO: grid.scrollTo(updatedObject);
            } catch (IllegalArgumentException ignore) {
            } catch (CrudOperationException e1) {
                refreshGrid();
            } catch (Exception e2) {
                refreshGrid();
                throw e2;
            }
        });
    }

    protected void deleteButtonClicked() {
        T domainObject = grid.asSingleSelect().getValue();
        showForm(CrudOperation.DELETE, domainObject, true, deletedMessage, event -> {
            try {
                deleteOperation.perform(domainObject);
                refreshGrid();
                grid.asSingleSelect().clear();
            } catch (CrudOperationException e1) {
                refreshGrid();
            } catch (Exception e2) {
                refreshGrid();
                throw e2;
            }
        });
    }

    protected void showForm(CrudOperation operation, T domainObject, boolean readOnly, String successMessage, ComponentEventListener<ClickEvent<Button>> buttonClickListener) {
        Component form = crudFormFactory.buildNewForm(operation, domainObject, readOnly, cancelClickEvent -> {
            if (clickRowToUpdate) {
                grid.asSingleSelect().clear();
            } else {
                T selected = grid.asSingleSelect().getValue();
                crudLayout.hideForm();
                grid.asSingleSelect().clear();
                grid.asSingleSelect().setValue(selected);
            }
        }, operationPerformedClickEvent -> {
            buttonClickListener.onComponentEvent(operationPerformedClickEvent);
            if (!clickRowToUpdate) {
                crudLayout.hideForm();
            }
            showNotification(successMessage);
        });
        String caption = crudFormFactory.buildCaption(operation, domainObject);
        crudLayout.showForm(operation, form, caption);
    }

    public void showNotification(String text) {
        if (showNotifications) {
            Notification.show(text);
        }
    }

}