import PageHeader from '../../components/layout/PageHeader'
import { Tabs } from '../../components/ui/Surface'
import StudentImportPanel from '../../features/account/StudentImportPanel'
import { studentImportLabels as labels } from '../../features/crud/resources'
import CrudPage from '../CrudPage'

export default function StudentImportPage() {
  return (
    <>
      <PageHeader title={labels.title} />
      <Tabs destroyOnHidden items={[
        { key: 'import', label: labels.tab, children: <StudentImportPanel /> },
        { key: 'profiles', label: labels.profiles, children: <CrudPage resource="adminStudents" /> },
      ]} />
    </>
  )
}
