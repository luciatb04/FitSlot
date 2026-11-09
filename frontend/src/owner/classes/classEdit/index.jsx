import React from "react";
import { useEffect, useState, useRef } from "react";
import { Link } from "react-router-dom";
import {
  Button,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "reactstrap";
import FormGenerator from "../../../components/formGenerator/formGenerator";
import { classEditFormInputs } from "./form/classEditFormInputs";
import "../../../static/css/owner/editClasses.css";
import "../../../static/css/auth/authButton.css"
import useFetchState from "../../../util/useFetchState";

export default function OwnerClassesEdit(){
  let pathArray = window.location.pathname.split("/");
  const emptyItem = {
    id: null,
    name: "",
    birthDate: "",
    type: {},
    owner: {},
  };  
  const jwt = JSON.parse(window.localStorage.getItem("jwt"));
  const [message,setMessage] = useState(null);
  const [modalShow,setModalShow] = useState(false);
  const [types, setTypes] = useState([])
  const [classEntity, setClassEntity] = useState(emptyItem);
  const [classId] = useState(pathArray[2]);
  const editClassesFormRef = useRef();
  
  useEffect( () => setupClasses(),[]);  
  
  function setupClasses(){
      if (classId !== "new" && classEntity.id == null) {
        fetch(
            `/api/v1/classes/${classId}`, 
            {
              headers: {
              Authorization: `Bearer ${jwt}`,
            },
          })
          .then((response) => response.json())
          .then((data) => {
            if(data.message){
              setMessage(data.message);
              setModalShow( true );
            }else {
              setClassEntity(data);
            }
          }).catch(m =>{
            setMessage(m);
            setModalShow( true );
          });          
    }    
    if(types.length===0){
      fetch(`/api/v1/classes/types`, {
          headers: {
            Authorization: `Bearer ${jwt}`,
          },
        }
      ).then(data => data.json()).then((data) => {
        if(!data.message)
          setTypes(data);
        else{
          setMessage(data.message);
          setModalShow(true);
        }
      }).catch(error => {setMessage(error);setModalShow(true);});
      }
  }

  function handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    const newClass = { ...classEntity };
    if (name === "type")
      newClass.type= types.filter(
                (type) => type.id === Number(value))[0];
    else 
      newClass[name] = value;
    setClassEntity(newClass);
  }  

  async function handleSubmit({ values }) {

    if (!editClassesFormRef.current.validate()) return;

    const classPayload = {
      id: classEntity.id,
      name: values["name"],
      birthDate: values["birthDate"],
      type: types.filter((type) => type.name === values["type"])[0],
      owner: classEntity.owner,
    };

    const submit = await (await fetch("/api/v1/classes" + (classEntity.id ? "/" + classId : ""),
      {
        method: classPayload.id ? "PUT" : "POST",
        headers: {
          Authorization: `Bearer ${jwt}`,
          Accept: "application/json",
          "Content-Type": "application/json",
        },
        body: JSON.stringify(classPayload),
      }
    )).json();

    if (submit.message){
      setMessage(submit.message);
      setModalShow(true);
    }
    else window.location.href = `/myClasses`;
  }
  
    const title = (
      <h2 className="text-center">{classEntity.id ? "Edit Class" : "Add Class"}</h2>
    );

    classEditFormInputs.forEach(i => i.handleChange=handleChange);
    

    if (classEditFormInputs[2].values.length < 2) {
      classEditFormInputs[2].values = [
        ...classEditFormInputs[2].values,
        ...types.map((type) => type.name),
      ];
    }

    if (classEntity && classEditFormInputs[2].values.length >= 2) {
      classEditFormInputs[0].defaultValue = classEntity.name || "";
      classEditFormInputs[1].defaultValue = classEntity.birthDate || "";
      classEditFormInputs[2].defaultValue = classEntity.type?.name || "None";
    }

    function handleShow() {
      setModalShow(false);
      setMessage(null);
    }

    let modal = <></>;
    if (message) {
      const show = modalShow;
      const closeBtn = (
        <button className="close" onClick={handleShow} type="button">
          &times;
        </button>
      );
      const cond = message.includes("limit");
      modal = (
        <div>
          <Modal isOpen={show} toggle={handleShow} keyboard={false}>
            {cond ? (
              <ModalHeader>Warning!</ModalHeader>
            ) : (
              <ModalHeader toggle={handleShow} close={closeBtn}>
                Error!
              </ModalHeader>
            )}
            <ModalBody>{message || ""}</ModalBody>
            <ModalFooter>
              <Button color="primary" tag={Link} to={`/myClasses`}>
                Back
              </Button>
            </ModalFooter>
          </Modal>
        </div>
      );
    }

    return (
      <div className="edit-class-page-container">
        <div className="edit-class-form-container">
          {title}
          <FormGenerator
            ref={editClassesFormRef}
            inputs={classEditFormInputs}
            onSubmit={handleSubmit}
            buttonText="Save"
            buttonClassName="auth-button"
          />
        </div>
        {modal}
      </div>
    );
  }
