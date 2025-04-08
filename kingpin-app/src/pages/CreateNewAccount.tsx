import { useState } from "react";
import { useDispatch } from "react-redux";
import { NavLink, useNavigate } from "react-router";
import { setUser } from "../store/userSlice";

export function CreateNewAccount() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [password2, setPassword2] = useState('');
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const login = async () => {
    if (password === password2) {
      dispatch(setUser(username));
      navigate("/");
    }
  };

  return (
    <>
      <div className="flex flex-col gap-4 items-center p-4">
        <div className="w-xs">
          <h1 className="text-xl">New Account</h1>
        </div>
        <input type="text" placeholder="Username" className="input w-xs" value={username}
               onChange={(e) => setUsername(e.target.value)}/>
        <input type="password" placeholder="Password" className="input" value={password}
               onChange={(e) => setPassword(e.target.value)}/>
        <input type="password" placeholder="Repeat Password" className="input" value={password2}
               onChange={(e) => setPassword2(e.target.value)}/>
        {/*<p className="fieldset-label">You can edit page title later on from settings</p>*/}
        <button className="btn btn-primary btn-sm" onClick={login}>Submit</button>
        <NavLink to="/login" className="link link-hover">Back to login...</NavLink>
      </div>
    </>
  )
}