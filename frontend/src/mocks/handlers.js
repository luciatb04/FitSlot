import { http, HttpResponse } from 'msw'

const authOwner = {
    "authority": "OWNER"
};

const userAdmin1 = {
    "id": 1,
    "username": "admin1",
    "authority": {
        "authority": "ADMIN"
    }
};

const userOwner1 = {
    "id": 2,
    "username": "owner1",
    "authority": authOwner
};

const userOwner2 = {
    "id": 3,
    "username": "owner2",
    "authority": authOwner
};

const userInstructor1 = {
    "id": 12,
    "username": "instructor1",
    "authority": {
        "authority": "INSTRUCTOR"
    }
};

const userInstructor2 = {
    "id": 13,
    "username": "instructor2",
    "authority": {
        "authority": "INSTRUCTOR"
    }
};

const gym1 = {
    "id": 1,
    "name": "Happy Classes Gym"
};

const gym2 = {
    "id": 2,
    "name": "Healthy Animals Gym"
};

const owner1 = {
    "id": 1,
    "firstName": "George",
    "lastName": "Franklin",
    "address": "110 W. Liberty St.",
    "city": "Sevilla",
    "telephone": "608555103",
    "plan": "PLATINUM",
    "gym": gym1,
    "user": userOwner1
};

const owner2 = {
    "id": 2,
    "firstName": "Betty",
    "lastName": "Davis",
    "address": "638 Cardinal Ave.",
    "city": "Sevilla",
    "telephone": "608555174",
    "plan": "PLATINUM",
    "gym": gym2,
    "user": userOwner2
};

const class1 = {
    "id": 1,
    "name": "Leo",
    "birthDate": "2010-09-07",
    "type": {
        "id": 1,
        "name": "cat"
    },
    "owner": owner1
};

const class2 = {
    "id": 2,
    "name": "Basil",
    "birthDate": "2012-08-06",
    "type": {
        "id": 6,
        "name": "hamster"
    },
    "owner": owner2
};

const instructor1 = {
    "id": 1,
    "firstName": "James",
    "lastName": "Carter",
    "specialties": [],
    "user": userInstructor1,
    "city": "Sevilla",
    "gym": gym1
}

const instructor2 = {
    "id": 2,
    "firstName": "Helen",
    "lastName": "Leary",
    "specialties": [
        {
            "id": 1,
            "name": "radiology"
        }
    ],
    "user": userInstructor2,
    "city": "Sevilla",
    "gym": gym2
};

const visit1 = {
    "id": 1,
    "datetime": "2013-01-01T13:00:00",
    "description": "rabies shot",
    "class": class1,
    "instructor": instructor1,
    "city": "Badajoz",
};

const visit2 = {
    "id": 2,
    "datetime": "2013-01-02T15:30:00",
    "description": "",
    "class": class1,
    "instructor": instructor2
};

const consultation1 = {
    "id": 1,
    "title": "Mi gato no come",
    "status": "ANSWERED",
    "owner": owner2,
    "class": class1,
    "creationDate": "2023-04-11T11:20:00"
};

const consultation2 = {
    "id": 2,
    "title": "Título 2",
    "status": "PENDING",
    "owner": owner1,
    "class": class1,
    "creationDate": "2023-04-11T11:20:00"
};

const ticket1 = {
    "id": 1,
    "description": "What vaccine should my dog recieve?",
    "creationDate": "2023-01-04T17:32:00",
    "user": userOwner1,
    "consultation": consultation1
};

const ticket2 = {
    "id": 2,
    "description": "Rabies' one.",
    "creationDate": "2023-01-04T17:36:00",
    "user": userInstructor1,
    "consultation": consultation1
}

export const handlers = [
    http.delete('*/:id', () => HttpResponse.json({ message: "Entity deleted" })),

    http.get('*/api/v1/owners', () => HttpResponse.json([owner1, owner2])),

    http.get('*/api/v1/classes', () => HttpResponse.json([class1, class2])),

    http.get('*/api/v1/users', () => HttpResponse.json([userAdmin1, userOwner1])),

    http.get('*/api/v1/instructors', () => HttpResponse.json([instructor1, instructor2])),

    http.get('*/api/v1/instructors/specialties', () => HttpResponse.json([
                {
                    "id": 1,
                    "name": "radiology"
                },
                {
                    "id": 2,
                    "name": "surgery"
                },
                {
                    "id": 3,
                    "name": "dentistry"
                }
            ])),

    http.get('*/api/v1/classes/:classId/visits', () => HttpResponse.json([visit1, visit2])),

    http.get('*/api/v1/consultations', () => HttpResponse.json([consultation1, consultation2])),

    http.get('*/api/v1/consultations/:id', () => HttpResponse.json([consultation1])),

    http.get('*/api/v1/consultations/:id/tickets', () => HttpResponse.json([ticket1, ticket2])),

    http.post('*/api/v1/consultations/:id/tickets', () => HttpResponse.json({
        "id": 3,
        "description": "test ticket",
        "creationDate": "2023-01-04T17:32:00",
        "user": userOwner1,
        "consultation": consultation1
    })),

    http.put('*/api/v1/consultations/:id', () => HttpResponse.json({
        "id": 1,
        "title": "Consulta sobre vacunas",
        "status": "CLOSED",
        "owner": owner1,
        "class": class1,
        "creationDate": "2023-01-04T17:30:00"
    })),

]
