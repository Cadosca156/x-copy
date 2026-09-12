import {apiFetch} from "./api";

export async function fetchNotes() {
    try {

        const response = await apiFetch(`/notes`);
        if (!response.ok) {
            console.error("API error:", response.status);
            return [];
        }
        const data = await response.json();
        console.log(data);
        console.log(response);
        return Array.isArray(data) ? data : [];
    }
    catch (error) {
        console.log(error);
        return [];
    }
}

export async function postNote(note) {
    try {

        const response = await apiFetch(`/notes`, {

            method: "POST",
            body: JSON.stringify(
                note
            )
        });
        if (!response.ok) {
            throw new Error("Failed to submit note");
        }
        return response.json();
    }
    catch (error) {
        console.log(error);
    }
}

export async function deleteNote(noteid) {
    try {

        const response = await apiFetch(`/notes/${noteid}`, {
            method: "DELETE"
        });
        if (!response.ok) {
            console.log("Deleting id:", noteid);
            throw new Error("Failed to delete note");
        }
    }
    catch (error) {
        console.log(error);
    }
}

export async function completeNote(note) {
    try {
        console.log("api сomplete:", note);
        const response = await apiFetch(`/notes/${note.id}`, {
            method: "PATCH",
            body: JSON.stringify(
                note
        )
        });
        if (!response.ok) {
            throw new Error("Failed to complete note");
        }


    }
    catch (error) {
        console.log(error);
    }
}
export async function updateNote(note) {
    try {
        console.log("api update:", note);
        const response = await apiFetch(`/notes/update/${note.id}`, {
            method: "PATCH",
            body: JSON.stringify(
                note
            )
        });
        if (!response.ok) {
            throw new Error("Failed to update note");
        }
    }
    catch (error) {
        console.log(error);
    }
}

