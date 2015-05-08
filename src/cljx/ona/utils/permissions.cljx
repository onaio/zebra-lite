(ns ona.utils.permissions
  (:use [ona.utils.seq :only [in?]])
  (:require [ona.helpers.permissions :as p]))

(defn get-user
  [seq username]
  (first (filter #(= (:user %) username) seq)))

(defn has-role?
  "Does the user role match the permission?"
  [user perm]
  (= (:role user) perm))

(defn get-role
  "Extract the role for a user on a given project"
  [users username]
  (:role (get-user users username)))

(defn get-owner
  "Return the owner from a list of users."
  [users]
  (first (filter #(= (:role %) p/owner) users)))

(defn get-perms
  "Extract the permissions for a user on a given project"
  [users username]
  (:permissions (get-user users username)))

(defn can-edit-project-settings?
  [role]
  (contains? #{p/owner p/manager} role))

(defn can-edit-settings?
  [users username]
  (can-edit-project-settings? (get-role users username)))

(defn can-add-form?
  [role]
  (contains? #{p/owner p/manager} role))

(defn can-edit-form?
  [role]
  (contains? #{p/owner p/manager} role))

(defn can-delete-form?
  [role]
  (contains? #{p/owner p/manager} role))

(defn can-add-data?
  [role]
  (contains? #{p/owner p/editor p/manager p/dataentry} role))

(defn can-edit-data?
  [role]
  (contains? #{p/owner p/editor p/manager} role))

(defn can-delete-data?
  [role]
  (contains? #{p/owner p/editor p/manager} role))

(defn has-perm?
  "Check whether user has specific permissions"
  [perms perm]
  (in? perms perm))

(defn has-role-name?
  ([users username role]
     (has-role?
      (get-user users username)
      role))
  ([user_role role]
     (= user_role role)))

(defn is-owner?
  ([users username]
     (has-role-name? users username p/owner))
  ([user_role]
     (has-role-name? user_role p/owner)))

(defn is-manager?
  ([users username]
    (has-role-name? users username p/manager))
  ([user_role]
    (has-role-name? user_role p/manager)))

(defn can-change-users? [role user username]
  (and (can-edit-project-settings? role)
       (or (not (and (has-role? user p/manager)
                     (= (:user user)
                        username)))
           (is-owner? role))))
