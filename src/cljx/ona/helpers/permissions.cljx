(ns ona.helpers.permissions)

;; Project user permissions
(def add_project_perm "add_project")

(def add_xform_perm "add_xform")

(def change_project_perm "change_project")

(def delete_project_perm "delete_project")

(def transfer_project_perm "transfer_project")

(def view_project_perm "view_project")

;; Permission settings constants
(def limited "limited")

(def open "open")

;; Roles
(def collaborator "collaborator")

(def member "member")

(def dataentry "dataentry")

(def editor "editor")

(def manager "manager")

(def owner "owner")

(def readonly "readonly")

(def hidden "hidden")

(def anonymous "")

;; Role to display name
(def roles {hidden "Cannot View (Hidden)"
            readonly "Can View (Read Only)"
            dataentry "Can Submit"
            editor "Can Edit"
            manager "Admin" ;TODO remove manager role after all previous managers are made owners on API/core
            owner "Admin"})

(def org-roles {owner "Admin"
                manager "Project Manager"
                editor "Member"
                collaborator "Collaborator"
                member "Member"})

(def assignable-org-roles (remove #(= (key %) member) org-roles))
