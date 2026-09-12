import {useEffect, useState} from "react";
import {fetchNotes, postNote, deleteNote, updateNote, completeNote} from "../../utils/NotesBDStorage";

import '../../styles/noteStyles/note.css';
import '../../styles/noteStyles/notes.css';
import '../../styles/noteStyles/notesBackground.css'
import '../../styles/noteStyles/cloudstyle.css';
import Navbar from "../Navbar";
import toast from "react-hot-toast";






export default function Notes() {

    const [formOpen, setFormOpen] = useState(false);
    const addnotenotify = () => toast('Added note successfully✅.');
    const deletenotenotify = () => toast('Deleted note successfully🗑️.');
    const completenotenotify = () => toast('Completed note successfully✅.');
    const editnotenotify = () => toast('Edited note successfully✏️.');

    const [editNote, setEditNote] = useState(false);
    const [notes,setNotes] = useState([]);
    const [note, setNote] = useState({
        text:"",
        date: "",
        completed: false,
    });

    const refreshNotes = async () => {
        const data = await fetchNotes();
        setNotes(data);
    };
    useEffect(() => {
        const loadNotes = async () => {
            try {
                const data = await fetchNotes();
                setNotes(data);
            } catch (error) {
                console.log(error);
            }
        };

        loadNotes();
    }, []);


    const handleSubmit = async () => {
if (note.text == null) {
    return
}
        await postNote(note)
        refreshNotes();
        setNotes(prev => [...prev, note]);
        setNote({
            text: "",
            date: "",
        });
        addnotenotify();
    }

    const handleDelete = async (noteid) => {
        await deleteNote(noteid)
        deletenotenotify();
        refreshNotes();

    }

    const handleComplete = async (note) => {

        const completedNote = {
            ...note,
            completed: !note.completed
        };

        setNotes(prev =>
            prev.map(n =>
                n.id === note.id
                    ? completedNote
                    : n
            )
        );
    completenotenotify();
        await completeNote(completedNote);
    }
    const handleUpdate = async (note) => {

        const updatedNote = {
            ...note,
            text: note.text,
            date: note.date,
        };


        setNote({
            text: "",
            date: "",
        });

        await updateNote(updatedNote);
        editnotenotify();
        refreshNotes();
    }

    function openForm() {
        setFormOpen(true);
    }

    function openEditNote(note) {
        setEditNote(true);

        setNote({
            id: note.id,
            text: note.text,
            date: note.date,
            completed: note.completed
        });
    }

    function formatDate(dateString) {
        if (!dateString) return "";
        const date = new Date(dateString);
        if (isNaN(date.getTime())) return "";

        return date.toLocaleString("uk-UA", {
            day: "2-digit",
            month: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        });
    }




    function getNoteStatus(date) {
        if (!date) return "normal";

        const now = Date.now();
        const dateTime = new Date(date).getTime();
        const TEN_MIN = 10 * 60 * 1000;

        if (now < dateTime) return "normal";
        if (now >= dateTime && now <= dateTime + TEN_MIN) return "warning";
        return "overdue";
    }

    const [, forceUpdate] = useState(0);

    useEffect(() => {
        const interval = setInterval(() => {
            forceUpdate(prev => prev + 1);
        }, 1000); // кожну секунду

        return () => clearInterval(interval);
    }, []);


    const sortedNotes = [...notes].sort((a, b) => {
        const statusA = getNoteStatus(a.date);
        const statusB = getNoteStatus(b.date);

        const priority = {
            overdue: 2,
            warning: 0,
            normal: 1,
        };

        return priority[statusA] - priority[statusB]

            ;
    });

    return (


        <div className="background">
<Navbar/>
                <button className="add-button" onClick={openForm}>
                    Add Note
                </button>







            <div className={"add-note"}>

                <form


                    onSubmit={(e) => {
                        e.preventDefault();
                        handleSubmit()
                    }}
                    className={`note-form ${formOpen ? "active" : ""}`}
                >
                    <div className={'note-label'}>Capture Your Thoughts</div>
                    <input
                        className="note_input"
                        type="text"
                        value={note.text}
                        style={{marginLeft: "10px",}}
                        onChange={(e) =>
                            setNote(prev => ({
                                ...prev,
                                text: e.target.value
                            }))
                        }
                        maxLength={99}
                        required
                    />
                    <input
                        className="note_input"
                        type="datetime-local"
                        value={note.date}
                        onChange={(e) => setNote({
                            ...note,
                            date: e.target.value
                        })}

                    />

                    <button className="sub-button" type="submit">
                        Submit
                    </button>
                    <button type="reset" className="cancel-button" onClick={() => {setFormOpen(false);
                        setNote({
                            text: "",
                            date: "",
                        });
                    }
                    }>
                        Cancel
                    </button>
                </form>
            </div>


            <ul className={"note-container"}>
                {sortedNotes.map((note) => (
                    <li key={note.id}
                        className={`note-li ${
                            note.completed
                                ? "note-completed"
                                : getNoteStatus(note.date) === "warning"
                                    ? "note-warning"
                                    : getNoteStatus(note.date) === "overdue"
                                        ? "note-overdue"
                                        : "note-normal"
                        }`}>
                        <div className="cloud__glow"></div>
                        <input
                            style={{transform: "scale(3)"}}
                            className={"completed-checkbox"}
                            type="checkbox"
                            checked={note.completed}
                            onChange={() =>
                                handleComplete(note)



                            }
                        />
                        <div className={"note-text"}>
                            <span style={{fontSize: "200%"}}>{note.text}</span>


                        </div>
                        <div className={"note-date"}>
                            <span style={{fontSize: "200%"}}> {formatDate(note.date)}</span>
                        </div>
                        <button
                            className="del-button"

                            onClick={() => {
                                handleDelete(note.id)
                            }}
                        >
                            Delete
                        </button>
                        <button className="edit-button"
                        onClick={() => openEditNote(note)}>Edit</button>

                    </li>

                ))}
            </ul>

            <div className={"note-editor"}>

                <form


                    onSubmit={(e) => {
                        e.preventDefault();
                        handleUpdate(note);
                        setEditNote(false);


                    }}
                    className={`note-edit-form ${editNote ? "active" : ""}`}
                >
                    <div className={'note-label'}>Edit Your Thoughts</div>
                    <input
                        className="note_input"
                        type="text"
                        value={note.text}
                        style={{marginLeft: "10px",}}
                        onChange={(e) =>
                            setNote(prev => ({
                                ...prev,
                                text: e.target.value
                            }))
                        }
                        maxLength={99}
                        required
                    />
                    <input
                        className="note_input"
                        type="datetime-local"
                        value={note.date}
                        onChange={(e) => setNote({
                            ...note,
                            date: e.target.value
                        })}

                    />

                    <button className="sub-button" type="submit">
                        Submit
                    </button>
                    <button type="reset" className="cancel-button" onClick={() => {setEditNote(false);
                        setNote({
                            text: "",
                            date: "",
                        });
                    }}>
                        Cancel
                    </button>
                </form>
            </div>




        </div>

    );
}