/**
 * Copyright (C) 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.dashboard.ui.panel.navigation.menu;

import org.apache.commons.lang.StringEscapeUtils;
import org.jboss.dashboard.LocaleManager;
import org.jboss.dashboard.ui.SessionManager;
import org.jboss.dashboard.ui.UIServices;
import org.jboss.dashboard.ui.taglib.formatter.Formatter;
import org.jboss.dashboard.ui.taglib.formatter.FormatterException;
import org.jboss.dashboard.users.UserStatus;
import org.jboss.dashboard.workspace.WorkspaceImpl;
import org.jboss.dashboard.workspace.Section;
import org.jboss.dashboard.security.SectionPermission;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class RenderMenuFormatter extends Formatter {

    private String beforeLink;
    private String afterLink;

    public void service(HttpServletRequest request, HttpServletResponse response) throws FormatterException {
        beforeLink = ((MenuDriver) getDriver()).getBeforeLink(getPanel());
        afterLink = ((MenuDriver) getDriver()).getAfterLink(getPanel());
        MenuDriver menuDriver = (MenuDriver) getPanel().getProvider().getDriver();
        String menuType = menuDriver.getMenuType(getPanel());
        if (MenuTypeListDataSupplier.PARAMETER_LANGUAGE.equals(menuType)) {
            renderLanguage(request, response, menuDriver);
        } else if (MenuTypeListDataSupplier.PARAMETER_WORKSPACE.equals(menuType)) {
            renderWorkspace(request, response, menuDriver);
        } else {
            renderPage(request, response, menuDriver);
        }
    }

    protected void renderLanguage(HttpServletRequest request, HttpServletResponse response, MenuDriver menuDriver) {
        renderOutputStart();
        Locale[] locales = getLocaleManager().getPlatformAvailableLocales();
        List selectedLangIds = menuDriver.getSelectedLangIds(getPanel());
        boolean isEditMode = SessionManager.getPanelSession(getPanel()).isEditMode();
        for (int j = 0; j < locales.length; j++) {
            Locale locale = locales[j];
            MenuItem menuItem = new LangMenuItem();
            menuItem.setId(locale.toString());
            Map text = new HashMap();
            text.put(LocaleManager.currentLang(), StringUtils.capitalize(locale.getDisplayName(locale)));
            menuItem.setText(text);
            menuItem.setUrl(menuDriver.getChangeLanguageLink(getPanel().getSection(), locale.toString()));
            menuItem.setSelected(Boolean.valueOf(selectedLangIds.contains(locale.toString())));
            menuItem.setVisible(Boolean.TRUE);
            if (locale.toString().equals(getLang())) {
                menuItem.setCurrent(Boolean.TRUE);
            } else {
                menuItem.setCurrent(Boolean.FALSE);
            }
            renderItem(menuItem, selectedLangIds.contains(MenuDriver.PARAMETER_ALL_ITEMS), isEditMode);
        }
        if (isEditMode)
            renderOutputEndButtons(selectedLangIds.contains(MenuDriver.PARAMETER_ALL_ITEMS), MenuDriver.ATTRIBUTE_SELECTED_LANG);
        renderOutputEnd();
    }

    protected void renderWorkspace(HttpServletRequest request, HttpServletResponse response, MenuDriver menuDriver) throws FormatterException {
        renderOutputStart();
        Set workspacesIdentifiers;
        try {
            workspacesIdentifiers = UIServices.lookup().getWorkspacesManager().getAvailableWorkspacesIds();
        } catch (Exception e) {
            throw new FormatterException("No available workspace identifiers: ", e);
        }

        List selectedWorkspaceIds = menuDriver.getSelectedWorkspaceIds(getPanel());
        boolean isEditMode = SessionManager.getPanelSession(getPanel()).isEditMode();
        if (workspacesIdentifiers != null && workspacesIdentifiers.size() > 0) {
            Iterator itWorkspacesIdentifiers = workspacesIdentifiers.iterator();
            while (itWorkspacesIdentifiers.hasNext()) {
                String id = (String) itWorkspacesIdentifiers.next();
                WorkspaceImpl workspace;
                try {
                    workspace = (WorkspaceImpl) UIServices.lookup().getWorkspacesManager().getWorkspace(id);
                } catch (Exception e) {
                    throw new FormatterException("No workspace found: ", e);
                }
                MenuItem menuItem = new WorkspaceMenuItem();
                menuItem.setId(workspace.getId());
                menuItem.setText(workspace.getName());
                menuItem.setUrl(menuDriver.getChangeWorkspaceLink(request, response, workspace.getId()));
                menuItem.setSelected(Boolean.valueOf(selectedWorkspaceIds.contains(workspace.getId())));
                menuItem.setVisible(Boolean.TRUE);
                if (workspace.getId().equals(getWorkspace().getId())) {
                    menuItem.setCurrent(Boolean.TRUE);
                } else {
                    menuItem.setCurrent(Boolean.FALSE);
                }
                renderItem(menuItem, selectedWorkspaceIds.contains(MenuDriver.PARAMETER_ALL_ITEMS), isEditMode);
            }
        }
        if (isEditMode)
            renderOutputEndButtons(selectedWorkspaceIds.contains(MenuDriver.PARAMETER_ALL_ITEMS), MenuDriver.ATTRIBUTE_SELECTED_WORKSPACE);
        renderOutputEnd();
    }

    protected void renderPage(HttpServletRequest request, HttpServletResponse response, MenuDriver menuDriver) {
        renderOutputStart();
        Section[] sections = ((WorkspaceImpl) getWorkspace()).getAllSections();
        List selectedPageIds = menuDriver.getSelectedPageIds(getPanel());
        boolean isEditMode = SessionManager.getPanelSession(getPanel()).isEditMode();
        if (sections != null && sections.length > 0) {
            for (int i = 0; i < sections.length; i++) {
                Section section = sections[i];
                if (section.isVisible().booleanValue()) {
                    MenuItem menuItem = new PageMenuItem();
                    menuItem.setId(section.getId().toString());
                    menuItem.setText(section.getTitle());
                    menuItem.setUrl(menuDriver.getChangePageLink(request, response, section));
                    menuItem.setSelected(Boolean.valueOf(selectedPageIds.contains(section.getId().toString())));
                    SectionPermission viewPerm = SectionPermission.newInstance(section, SectionPermission.ACTION_VIEW);
                    boolean canView = UserStatus.lookup().hasPermission(viewPerm);
                    if (canView) {
                        menuItem.setVisible(Boolean.TRUE);
                    } else {
                        menuItem.setVisible(Boolean.FALSE);
                    }
                    if (section.getId().equals(getSection().getId())) {
                        menuItem.setCurrent(Boolean.TRUE);
                    } else {
                        menuItem.setCurrent(Boolean.FALSE);
                    }
                    renderItem(menuItem, selectedPageIds.contains(MenuDriver.PARAMETER_ALL_ITEMS), isEditMode);
                }
            }
        }
        if (isEditMode)
            renderOutputEndButtons(selectedPageIds.contains(MenuDriver.PARAMETER_ALL_ITEMS), MenuDriver.ATTRIBUTE_SELECTED_PAGE);
        renderOutputEnd();
    }

    private void renderOutputEndButtons(boolean allItemsSelected, String inputName) {
        writeToOut(beforeLink);
        setAttribute("inputName", inputName);
        setAttribute("allItemsSelected", allItemsSelected ? "checked" : "");
        renderFragment("outputAllItemsCheckbox");
        writeToOut(afterLink);
        writeToOut(beforeLink);
        renderFragment("submitButton");
        writeToOut(afterLink);
    }

    protected void renderItem(MenuItem menuItem, boolean allItemsSelected, boolean editMode) {
        if (((menuItem.isVisible()) && (allItemsSelected || menuItem.isSelected())) || editMode) {
            writeToOut(beforeLink);
            setAttribute("text", StringEscapeUtils.escapeHtml(getLocalizedValue(menuItem.getText())));
            setAttribute("url", menuItem.getUrl());
            setAttribute("itemId", menuItem.getId());
            setAttribute("allItems", allItemsSelected);
            setAttribute("inputName", menuItem.getItemInputName());
            renderFragment(menuItem.isCurrent() ? "outputCurrent" : "outputNotCurrent");
            renderFragment(menuItem.isSelected() ? "outputSelected" : "outputNotSelected");
            writeToOut(afterLink);
        }
    }

    protected void renderOutputStart() {
        String before = ((MenuDriver) getDriver()).getStartHTML(getPanel());
        writeToOut(before);
        renderFragment("outputStart");
    }

    protected void renderOutputEnd() {
        renderFragment("outputEnd");
        String after = ((MenuDriver) getDriver()).getEndHTML(getPanel());
        writeToOut(after);
    }
}
