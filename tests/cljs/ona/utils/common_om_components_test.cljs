(ns ona.utils.common-om-components-test
  (:require-macros [cljs.test :refer (is deftest testing)]
                   [dommy.macros :refer [node sel sel1]])
  (:require [cljs.test]
            [dommy.core :as dommy]
            [cljs.core.async :refer [<! chan put!]]
            [om.core :as om :include-macros true]
            [ona.utils.dom :as dom-utils]
            [ona.utils.common-om-components :as common]
            [ona.helpers.permissions :as p]))

;; RENDER HELPER

(defn- add-submission-container
  [id role]
  "Returns a container in which a table has been rendered."
  (common/component->container common/add-submission-button nil
                               {:opts {:role role :auth-token nil :class "cls"}}))

;; TESTS

(deftest iframe-tests
         (let [src (gensym)
               c (dom-utils/new-container!)
               ch (chan 1)
               a (atom nil)]
           (testing "inactive iframes don't render, but run on-finish directly"
                    (om/root common/iframe-modal nil
                             {:target c
                              :init-state {:closed true}
                              :opts {:remove-chan ch :on-finish #(reset! a 1)}})
                    (is (not (sel1 :iframe)))
                    (is (= 1 @a)))
           (testing "iframe renders on page when new-iframe-modal! is called"
                    (common/new-iframe-modal! src identity)
                    (is (sel1 :iframe))
                    (is (= (str src) (-> (sel1 :iframe) (dommy/attr :src))))
                    (is (-> (sel1 :iframe) (aget "onload"))))))

(defn stage->btn [stage & [opts]]
  (let [state (merge {:stage stage}
                     (when (contains? #{:file-selected :uploading :done} stage)
                       {:file "FILENAME"}))]
    (common/component->container
      common/file-upload-button
      nil

      {:opts (merge {:btn-text "CHOOSE"
                     :filetype "image/jpeg"
                     :input-name "media-file"
                     :url "URL"
                     :success-cb identity
                     :messages {:initial "INITIAL"
                                :file-selected "SELECTED"
                                :error [:p.status-msg.error "ERROR"]
                                :uploading "UPLOADING"
                                :done "DONE:"}}
                    opts)
       :init-state state})))

(deftest file-upload-button-test
         (let [initial-btn (stage->btn :initial)
               selected-btn (stage->btn :file-selected)
               uploading-btn (stage->btn :uploading)
               error-btn (stage->btn :error)
               done-btn (stage->btn :done)]
           (testing "In initial stage, there should be a visible input, msg"
                    (let [form (sel1 initial-btn :form)
                          file-input (sel1 form :input)]
                      ;; Visible input button
                      (is (= "file" (dommy/attr file-input :type)))
                      (is (= "image/jpeg" (dommy/attr file-input :accept)))
                      (is (= "media-file" (dommy/attr file-input :name)))
                      (is (= "URL" (dommy/attr form :action)))
                      (is (nil? (-> form (sel1 :.btn-upload) (dommy/attr :style))))
                      ;; Message + button
                      (is (re-find #"CHOOSE" (dommy/text form)))
                      (is (= "INITIAL" (-> form (sel1 :span) dommy/text)))))
           (testing "While file selected, should have: insivible file input,
      filename, .rm-file,upload button, and msg"
                    (let [form (sel1 selected-btn :form)
                          file-input (sel1 selected-btn :input)]
                      ;; Invisible input button
                      (is (= "file" (dommy/attr file-input :type)))
                      (is (= "image/jpeg" (dommy/attr file-input :accept)))
                      (is (= "media-file" (dommy/attr file-input :name)))
                      (is (= "URL" (dommy/attr form :action)))
                      (is (= "display:none;"
                             (-> form (sel1 :.btn-upload) (dommy/attr :style))))
                      ;; Filename, "Upload", msg all present
                      (is (sel1 selected-btn :.rm-file))
                      (is (re-find #"SELECTED" (dommy/text selected-btn)))
                      (is (re-find #"Upload" (dommy/text selected-btn)))
                      (is (re-find #"FILENAME" (dommy/text selected-btn)))))
           (testing "While uploading, should have: file name, spinner."
                    (let [form (sel1 uploading-btn :form)
                          file-input (sel1 uploading-btn :input)]
                      ;; Input button still invisible
                      (is (= "display:none;"
                             (-> form (sel1 :.btn-upload) (dommy/attr :style))))
                      ;; Filename, msg, spinner present, no rm-file option
                      (is (re-find #"FILENAME" (dommy/text form)))
                      (is (re-find #"UPLOADING" (dommy/text form)))
                      (is (nil? (sel1 form :.rm-file)))
                      (is (sel1 form :i.fa.fa-spin))))
           (testing "In error state, similar to initial + error message."
                    (let [form (sel1 error-btn :form)
                          file-input (sel1 error-btn :input)]
                      (is (sel1 form :p.status-msg.error))
                      (is (= "ERROR" (-> form (sel1 :p.status-msg.error) dommy/text)))
                      ;; Otherwiser, Similar to initial-btn
                      (is (= "file" (dommy/attr file-input :type)))
                      (is (= "image/jpeg" (dommy/attr file-input :accept)))
                      (is (= "media-file" (dommy/attr file-input :name)))
                      (is (= "URL" (dommy/attr form :action)))
                      (is (nil? (-> form (sel1 :.btn-upload) (dommy/attr :style))))
                      (is (re-find #"CHOOSE" (dommy/text form)))))
           (testing "In done state, should only display message + filename."
                    (let [form (sel1 done-btn :form)]
                      ;; Input button invisible
                      (is (= "display:none;"
                             (-> form (sel1 :.btn-upload) (dommy/attr :style))))
                      ;; Filename, msg, no spinner, no rm-file option
                      (is (re-find #"FILENAME" (dommy/text form)))
                      (is (re-find #"DONE:" (dommy/text form)))
                      (is (nil? (sel1 form :.rm-file)))
                      (is (not (sel1 form :i.fa.fa-spin)))))))

(deftest file-replace-button-test
         (let [stage->replace-button #(stage->btn % {:pre-existing-filename "PRE"})]
           (testing "In initial stage, :pre-existing-filename is there"
                    (let [form (sel1 (stage->replace-button :initial) :form)]
                      (is (re-find #"PRE" (dommy/text form)))))
           (testing "In other stages, :pre-existing-filename is not there"
                    (let [form-u (sel1 (stage->replace-button :uploading) :form)
                          form-f (sel1 (stage->replace-button :file-selected) :form)
                          form-e (sel1 (stage->replace-button :error) :form)
                          form-d (sel1 (stage->replace-button :d) :form)]
                      (is (not (re-find #"PRE" (dommy/text form-u))))
                      (is (not (re-find #"PRE" (dommy/text form-f))))
                      (is (not (re-find #"PRE" (dommy/text form-e))))
                      (is (not (re-find #"PRE" (dommy/text form-d))))))))
