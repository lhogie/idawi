import JSONViewer from './JSONViewer';

export default function ExceptionComponent({data, message}){

    return (
        <>
            <p> Message : {message} </p>
            <JSONViewer data={data} />
        </>
    )
}